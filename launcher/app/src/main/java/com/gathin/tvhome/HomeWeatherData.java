package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Pure data/parsing/refresh policy for the personal weather build. No Android dependencies. */
final class HomeWeatherData {
    static final long REFRESH_MS = 15 * 60_000L;
    static final long RETRY_MS = 5 * 60_000L;
    static final long MANUAL_MIN_MS = 60_000L;
    static final long LOCATION_MS = 6 * 60 * 60_000L;
    static final long STALE_MS = 30 * 60_000L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private HomeWeatherData() {}

    static final class Place {
        final String name;
        final double latitude, longitude;

        Place(String name, double latitude, double longitude) {
            if (name == null || name.trim().isEmpty() || !Double.isFinite(latitude)
                    || !Double.isFinite(longitude) || latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Invalid weather place");
            }
            this.name = name.replaceAll("[\\p{Cntrl}]", " ").trim();
            // IP location is approximate. Weather does not need more precise coordinates.
            this.latitude = Math.round(latitude * 100.0) / 100.0;
            this.longitude = Math.round(longitude * 100.0) / 100.0;
        }

        JSONObject json() throws JSONException {
            return new JSONObject().put("name", name).put("latitude", latitude).put("longitude", longitude);
        }

        static Place read(JSONObject json) throws JSONException {
            return new Place(json.getString("name"), json.getDouble("latitude"), json.getDouble("longitude"));
        }
    }

    static final class Report {
        final Place place;
        final double temperature, wind;
        final int humidity, code;
        final long observedAt, fetchedAt;

        Report(Place place, double temperature, int humidity, double wind, int code,
               long observedAt, long fetchedAt) {
            if (place == null || !Double.isFinite(temperature) || temperature < -100 || temperature > 70
                    || humidity < -1 || humidity > 100 || !Double.isFinite(wind)
                    || wind < -1 || wind > 500 || observedAt <= 0 || fetchedAt <= 0) {
                throw new IllegalArgumentException("Invalid weather report");
            }
            this.place = place;
            this.temperature = temperature;
            this.humidity = humidity;
            this.wind = wind;
            this.code = code;
            this.observedAt = observedAt;
            this.fetchedAt = fetchedAt;
        }

        JSONObject json() throws JSONException {
            return new JSONObject().put("place", place.json()).put("temperature", temperature)
                    .put("humidity", humidity).put("wind", wind).put("code", code)
                    .put("observedAt", observedAt).put("fetchedAt", fetchedAt);
        }

        static Report read(JSONObject json) throws JSONException {
            return new Report(Place.read(json.getJSONObject("place")), json.getDouble("temperature"),
                    json.getInt("humidity"), json.getDouble("wind"), json.getInt("code"),
                    json.getLong("observedAt"), json.getLong("fetchedAt"));
        }
    }

    static Place ipPlace(JSONObject reply) throws JSONException {
        if (!reply.optBoolean("success", false)) throw new JSONException("IP location unavailable");
        String name = clean(reply.optString("city", ""));
        if (name.isEmpty()) name = clean(reply.optString("region", ""));
        if (name.isEmpty()) name = clean(reply.optString("country", ""));
        return new Place(name, reply.getDouble("latitude"), reply.getDouble("longitude"));
    }

    private static String clean(String text) {
        return text == null || "null".equals(text) ? "" : text.trim();
    }

    static Report weather(JSONObject reply, Place place, long now) throws JSONException {
        if (reply.optBoolean("error", false)) throw new JSONException("Weather provider error");
        JSONObject current = reply.getJSONObject("current");
        // Required values use strict getters: JSON null must never become a sunny 0 degrees.
        double temp = current.getDouble("temperature_2m");
        int code = current.getInt("weather_code");
        long seconds = current.getLong("time");
        if (seconds < 1_000_000_000L || seconds > 10_000_000_000L) {
            throw new JSONException("Invalid current-weather timestamp");
        }
        int humidity = current.isNull("relative_humidity_2m") ? -1 : current.getInt("relative_humidity_2m");
        double wind = current.isNull("wind_speed_10m") ? -1 : current.getDouble("wind_speed_10m");
        return new Report(place, temp, humidity, wind, code, seconds * 1000L, now);
    }

    static String url(Place place) {
        return String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f"
                        + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                        + "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=auto&timeformat=unixtime&forecast_days=1",
                place.latitude, place.longitude);
    }

    static boolean fresh(long now, long timestamp, long interval) {
        return timestamp > 0 && now >= timestamp && now - timestamp < interval;
    }

    /** Persisted attempt times prevent repeated Home opens from causing a request storm. */
    static long delay(long now, long lastAttempt, long fetchedAt, boolean failed, boolean force) {
        if (force) return remaining(now, lastAttempt, MANUAL_MIN_MS);
        if (failed) return remaining(now, lastAttempt, RETRY_MS);
        return Math.max(remaining(now, fetchedAt, REFRESH_MS), remaining(now, lastAttempt, MANUAL_MIN_MS));
    }

    private static long remaining(long now, long then, long interval) {
        return fresh(now, then, interval) ? interval - (now - then) : 0;
    }

    static String compactText(Report report, long now, boolean failed, boolean differentNetwork) {
        boolean stale = failed || differentNetwork || !fresh(now, report.fetchedAt, STALE_MS)
                || !fresh(now, report.observedAt, 90 * 60_000L);
        String status = differentNetwork ? " · Khác mạng" : stale ? " · Dữ liệu cũ" : "";
        // Put stale status first so a long place name cannot ellipsize away the cache warning.
        return Math.round(report.temperature) + "°C " + condition(report.code) + status + " · " + report.place.name;
    }

    static String text(Report report, long now, boolean failed, boolean differentNetwork, ZoneId zone) {
        boolean stale = failed || differentNetwork || !fresh(now, report.fetchedAt, STALE_MS)
                || !fresh(now, report.observedAt, 90 * 60_000L);
        String first = report.place.name + " \u00b7 " + Math.round(report.temperature) + "\u00b0C \u00b7 "
                + condition(report.code);
        StringBuilder second = new StringBuilder();
        if (report.humidity >= 0) second.append("\u0110\u1ed9 \u1ea9m ").append(report.humidity).append('%');
        if (report.wind >= 0) {
            if (second.length() > 0) second.append(" \u00b7 ");
            second.append("Gi\u00f3 ").append(Math.round(report.wind)).append(" km/h");
        }
        if (second.length() > 0) second.append(" \u00b7 ");
        second.append(stale ? "D\u1eef li\u1ec7u c\u0169 " : "C\u1eadp nh\u1eadt ");
        second.append(TIME.withZone(zone).format(Instant.ofEpochMilli(report.fetchedAt)));
        return first + "\n" + second;
    }

    static String condition(int code) {
        switch (code) {
            case 0: return "Tr\u1eddi quang";
            case 1: return "\u00cdt m\u00e2y";
            case 2: return "C\u00f3 m\u00e2y";
            case 3: return "\u00c2m u";
            case 45: case 48: return "S\u01b0\u01a1ng m\u00f9";
            case 51: case 53: case 55: return "M\u01b0a ph\u00f9n";
            case 56: case 57: return "M\u01b0a ph\u00f9n b\u0103ng gi\u00e1";
            case 61: return "M\u01b0a nh\u1eb9";
            case 63: return "M\u01b0a v\u1eeba";
            case 65: return "M\u01b0a to";
            case 66: case 67: return "M\u01b0a b\u0103ng gi\u00e1";
            case 71: case 73: case 75: case 77: return "Tuy\u1ebft";
            case 80: case 81: case 82: return "M\u01b0a r\u00e0o";
            case 85: case 86: return "Tuy\u1ebft r\u00e0o";
            case 95: return "D\u00f4ng";
            case 96: case 99: return "D\u00f4ng, m\u01b0a \u0111\u00e1";
            default: return "Ch\u01b0a r\u00f5 tr\u1ea1ng th\u00e1i";
        }
    }
}
