package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

/**
 * Personal launcher extension. Approximate place: ipwho.is; current model weather: Open-Meteo.
 * HTTPS, no keys, GPS, extra location grants, ads, background service or third-party SDK.
 * One worker; bounded requests run off the UI thread. Scheduling exists only while Home is resumed.
 */
final class HomeWeather {
    private static final String PREFS = "personal_home_weather_v1";
    private static final String IP_URL = "https://ipwho.is/";
    private static final int TIMEOUT_MS = 8_000;
    private static final int MAX_BYTES = 128 * 1024;
    private final Activity activity;
    private final TextView view;
    private final SharedPreferences prefs;
    private final ConnectivityManager connectivity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean closed, registered, failed, forcePending;
    private final Runnable refresh = () -> request(forcePending);
    private volatile boolean active;
    private volatile int generation;
    private volatile HttpsURLConnection connection;
    private long lastAttempt, locatedAt, placeNetwork, network;
    private HomeWeatherData.Place place;
    private HomeWeatherData.Report report;
    private Future<?> task;
    private AlertDialog details;

    HomeWeather(Activity activity, TextView view) {
        this.activity = activity;
        this.view = view;
        this.connectivity = activity.getSystemService(ConnectivityManager.class);
        this.prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        lastAttempt = prefs.getLong("attempt", 0);
        locatedAt = prefs.getLong("located", 0);
        placeNetwork = prefs.getLong("place_network", 0);
        failed = prefs.getBoolean("failed", false);
        try {
            String raw = prefs.getString("place", "");
            if (!raw.isEmpty()) place = HomeWeatherData.Place.read(new JSONObject(raw));
            raw = prefs.getString("report", "");
            if (!raw.isEmpty()) report = HomeWeatherData.Report.read(new JSONObject(raw));
        } catch (Exception ignored) {
            // An unreadable cache is optional; it must not prevent the launcher from opening.
            place = null;
            report = null;
            locatedAt = 0;
        }
        view.setOnClickListener(v -> showDetails());
        render();
    }

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network available) {
            main.post(() -> {
                if (!active || closed) return;
                long current = available.getNetworkHandle();
                if (current != network) {
                    network = current;
                    cancelTask();
                    render();
                    request(true);
                }
            });
        }
    };

    void start() {
        if (closed || active) return;
        active = true;
        generation++;
        try {
            Network current = connectivity == null ? null : connectivity.getActiveNetwork();
            network = current == null ? 0 : current.getNetworkHandle();
            if (connectivity != null) {
                connectivity.registerDefaultNetworkCallback(networkCallback);
                registered = true;
            }
        } catch (RuntimeException ignored) {
            registered = false;
        }
        render();
        request(place != null && placeNetwork != network);
    }

    void stop() {
        active = false;
        main.removeCallbacks(refresh);
        cancelTask();
        if (registered && connectivity != null) {
            try { connectivity.unregisterNetworkCallback(networkCallback); }
            catch (RuntimeException ignored) { }
            registered = false;
        }
        if (details != null) {
            details.dismiss();
            details = null;
        }
    }

    void close() {
        if (closed) return;
        stop();
        closed = true;
        worker.shutdownNow();
    }

    private void cancelTask() {
        generation++;
        if (task != null) task.cancel(true);
        task = null;
        HttpsURLConnection current = connection;
        if (current != null) current.disconnect();
    }

    private boolean current(int token) {
        return active && token == generation && !Thread.currentThread().isInterrupted();
    }

    private void request(boolean force) {
        if (force) forcePending = true;
        if (!active || closed || (task != null && !task.isDone())) return;
        force = forcePending;
        long now = System.currentTimeMillis();
        long delay = HomeWeatherData.delay(now, lastAttempt,
                report == null ? 0 : report.fetchedAt, failed, force);
        if (delay > 0) {
            render();
            schedule(delay);
            return;
        }
        main.removeCallbacks(refresh);
        forcePending = false;
        lastAttempt = now;
        prefs.edit().putLong("attempt", now).apply();
        int token = ++generation;
        long requestedNetwork = network;
        HomeWeatherData.Place savedPlace = place;
        long savedLocatedAt = locatedAt;
        boolean locate = savedPlace == null || placeNetwork != requestedNetwork
                || !HomeWeatherData.fresh(now, savedLocatedAt, HomeWeatherData.LOCATION_MS);
        task = worker.submit(() -> {
            HomeWeatherData.Place resolved = savedPlace;
            long resolvedAt = savedLocatedAt;
            HomeWeatherData.Report result = null;
            try {
                if (locate) {
                    resolved = HomeWeatherData.ipPlace(get(IP_URL, token));
                    resolvedAt = System.currentTimeMillis();
                }
                if (!current(token)) return;
                result = HomeWeatherData.weather(get(HomeWeatherData.url(resolved), token),
                        resolved, System.currentTimeMillis());
            } catch (Exception ignored) {
                // Keep the last valid report and mark it old; never replace errors with zero degrees.
            }
            HomeWeatherData.Place finalPlace = resolved;
            long finalLocatedAt = resolvedAt;
            HomeWeatherData.Report finalReport = result;
            main.post(() -> {
                if (!active || closed || generation != token) return;
                task = null;
                SharedPreferences.Editor editor = prefs.edit();
                if (finalPlace != null && (!locate || finalLocatedAt != savedLocatedAt)) {
                    place = finalPlace;
                    locatedAt = finalLocatedAt;
                    placeNetwork = requestedNetwork;
                    try {
                        editor.putString("place", place.json().toString()).putLong("located", locatedAt)
                                .putLong("place_network", placeNetwork);
                    } catch (Exception ignored) { }
                }
                failed = finalReport == null;
                if (finalReport != null) {
                    report = finalReport;
                    try { editor.putString("report", report.json().toString()); }
                    catch (Exception ignored) { }
                }
                editor.putBoolean("failed", failed).apply();
                render();
                schedule(failed ? HomeWeatherData.RETRY_MS : HomeWeatherData.REFRESH_MS);
            });
        });
    }

    private JSONObject get(String address, int token) throws Exception {
        if (!current(token)) throw new InterruptedIOException();
        HttpsURLConnection http = (HttpsURLConnection) new URL(address).openConnection();
        connection = http;
        try {
            http.setConnectTimeout(TIMEOUT_MS);
            http.setReadTimeout(TIMEOUT_MS);
            http.setInstanceFollowRedirects(false);
            http.setUseCaches(false);
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("User-Agent", "PersonalCleanYourTV-Weather/1.0");
            if (!current(token)) throw new InterruptedIOException();
            if (http.getResponseCode() != 200) throw new IOException("Weather HTTP error");
            if (http.getContentLength() > MAX_BYTES) throw new IOException("Weather response too large");
            try (InputStream in = http.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (!current(token)) throw new InterruptedIOException();
                    if (bytes.size() + n > MAX_BYTES) throw new IOException("Weather response too large");
                    bytes.write(buffer, 0, n);
                }
                return new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            http.disconnect();
            if (connection == http) connection = null;
        }
    }

    private void schedule(long delay) {
        main.removeCallbacks(refresh);
        if (active && !closed) main.postDelayed(refresh, Math.max(1000, delay));
    }

    private void render() {
        String text;
        if (report != null) {
            text = HomeWeatherData.compactText(report, System.currentTimeMillis(), failed,
                    placeNetwork != network);
        } else {
            text = failed ? "Th\u1eddi ti\u1ebft: ch\u01b0a c\u1eadp nh\u1eadt \u0111\u01b0\u1ee3c\nNh\u1ea5n OK \u0111\u1ec3 th\u1eed l\u1ea1i"
                    : "Th\u1eddi ti\u1ebft: \u0111ang x\u00e1c \u0111\u1ecbnh khu v\u1ef1c...";
        }
        view.setText(text);
    }

    private void showDetails() {
        if (!active || activity.isFinishing() || activity.isDestroyed()) return;
        String fullWeather = report == null ? "" : HomeWeatherData.text(report, System.currentTimeMillis(), failed,
                placeNetwork != network, ZoneId.of("Asia/Ho_Chi_Minh")) + "\n\n";
        String message = fullWeather + "V\u1ecb tr\u00ed \u0111\u01b0\u1ee3c \u01b0\u1ee3ng qua IP m\u1ea1ng b\u1edfi ipwho.is, "
                + "kh\u00f4ng ph\u1ea3i GPS. Nh\u00e0 m\u1ea1ng ho\u1eb7c VPN c\u00f3 th\u1ec3 l\u00e0m l\u1ec7ch khu v\u1ef1c.\n\n"
                + "Th\u1eddi ti\u1ebft: https://open-meteo.com/ (CC BY 4.0, d\u1eef li\u1ec7u m\u00f4 h\u00ecnh). "
                + "T\u1ef1 l\u00e0m m\u1edbi m\u1ed7i 15 ph\u00fat khi Home \u0111ang m\u1edf; "
                + "t\u00ecm l\u1ea1i v\u1ecb tr\u00ed khi \u0111\u1ed5i m\u1ea1ng ho\u1eb7c sau 6 gi\u1edd.\n\n"
                + "L\u00e0m m\u1edbi s\u1ebd t\u00ecm l\u1ea1i khu v\u1ef1c; t\u1ed1i \u0111a m\u1ed9t l\u1ea7n/ph\u00fat.";
        details = new AlertDialog.Builder(activity)
                .setTitle("Th\u1eddi ti\u1ebft t\u1ef1 \u0111\u1ed9ng")
                .setMessage(message)
                .setPositiveButton("L\u00e0m m\u1edbi", (dialog, which) -> {
                    locatedAt = 0;
                    prefs.edit().putLong("located", 0).apply();
                    cancelTask();
                    request(true);
                })
                .setNegativeButton("\u0110\u00f3ng", null)
                .create();
        details.show();
    }
}
