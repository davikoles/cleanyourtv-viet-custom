package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Opt-in parental blackout, not device-owner/kiosk security. Never reads accessibility node content.
 * Filtering is passive while unlocked; during blackout it consumes keys Android delivers to it.
 */
public final class ChildLockService extends AccessibilityService {
    private static ChildLockService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ChildLockRules.Secret secret = new ChildLockRules.Secret();
    private final ChildLockRules.KeyPairs keyPairs = new ChildLockRules.KeyPairs();
    private final Runnable tick = this::reconcile;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (prefs, key) -> queue();
    private SharedPreferences prefs;
    private WindowManager windows;
    private ChildLockAudio audio;
    private FrameLayout blackout;
    private ChildLockPinPad pinPad;
    private BroadcastReceiver unlockReceiver;
    private boolean initialised, cleaned;

    static boolean ready() { return instance != null && instance.initialised && !keyFilterConflict(instance); }

    static boolean keyFilterConflict(Context context) {
        AccessibilityManager manager = context.getSystemService(AccessibilityManager.class);
        if (manager == null) return false;
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            String id = info.getId();
            ComponentName name = id == null ? null : ComponentName.unflattenFromString(id);
            boolean ours = name != null && name.getPackageName().equals(context.getPackageName())
                    && name.getClassName().equals(ChildLockService.class.getName());
            if (!ours && (info.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0) return true;
        }
        return false;
    }

    static boolean enabled(Context context) {
        String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (services == null) return false;
        for (String entry : services.split(":")) {
            ComponentName name = ComponentName.unflattenFromString(entry);
            if (name != null && name.getPackageName().equals(context.getPackageName())
                    && name.getClassName().equals(ChildLockService.class.getName())) return true;
        }
        return false;
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        cleaned = false;
        UserManager user = getSystemService(UserManager.class);
        if (user == null || user.isUserUnlocked()) { initialise(); return; }
        unlockReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                stopWaiting();
                initialise();
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED), Context.RECEIVER_NOT_EXPORTED);
        } else registerReceiver(unlockReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        if (user.isUserUnlocked()) { stopWaiting(); initialise(); }
    }

    private void stopWaiting() {
        if (unlockReceiver == null) return;
        unregisterReceiver(unlockReceiver);
        unlockReceiver = null;
    }

    private void initialise() {
        if (initialised) return;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null || (info.getCapabilities() & AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS) == 0) {
            Toast.makeText(this, "Box ch\u01b0a c\u1ea5p kh\u1ea3 n\u0103ng ch\u1eb7n ph\u00edm", Toast.LENGTH_LONG).show();
            return;
        }
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.eventTypes |= AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        setServiceInfo(info);
        prefs = ChildLockState.prefs(this);
        windows = getSystemService(WindowManager.class);
        audio = new ChildLockAudio(this);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        initialised = true;
        instance = this;
        reconcile();
    }

    private void queue() {
        handler.removeCallbacks(tick);
        if (initialised) handler.post(tick);
    }

    private void reconcile() {
        handler.removeCallbacks(tick);
        if (!initialised) return;
        if ((ChildLockState.locked(this) || ChildLockState.timed(this)) && keyFilterConflict(this)) {
            ChildLockState.error(this, "D\u1ecbch v\u1ee5 kh\u00e1c \u0111ang ch\u1eb7n ph\u00edm; \u0111\u00e3 h\u1ee7y kh\u00f3a");
            removeBlackout();
            audio.release(true);
            Toast.makeText(this, "Xung \u0111\u1ed9t d\u1ecbch v\u1ee5 ph\u00edm; \u0111\u00e3 h\u1ee7y kh\u00f3a", Toast.LENGTH_LONG).show();
            return;
        }
        if (!ChildLockState.locked(this) && ChildLockState.timed(this) && ChildLockState.remaining(this) <= 0) {
            if (!ChildLockState.lock(this)) {
                ChildLockState.error(this, "Kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c tr\u1ea1ng th\u00e1i kh\u00f3a");
            }
        }
        if (ChildLockState.locked(this)) {
            if (blackout == null) {
                if (!showBlackout()) return;
                if (!audio.engage()) {
                    ChildLockState.error(this, "Kh\u00f4ng ghi nh\u1edb \u0111\u01b0\u1ee3c \u00e2m l\u01b0\u1ee3ng; \u0111\u00e3 h\u1ee7y kh\u00f3a");
                    removeBlackout();
                    audio.release(true);
                    return;
                }
            } else audio.maintain();
            if (pinPad != null) {
                pinPad.updateCooldown();
                if (pinPad.idleMillis() >= 45_000) hidePin();
            }
            handler.postDelayed(tick, pinPad == null ? 5000 : 1000);
        } else {
            removeBlackout();
            audio.release(true);
            if (ChildLockState.timed(this)) {
                handler.postDelayed(tick, Math.max(100, Math.min(15_000, ChildLockState.remaining(this))));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean showBlackout() {
        if (windows == null) { overlayFailed(); return false; }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setFocusableInTouchMode(true);
        root.setClickable(true);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.OPAQUE);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setTitle("CleanYourTV Child Lock");
        if (Build.VERSION.SDK_INT >= 28) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        try {
            windows.addView(root, params);
            blackout = root;
            secret.reset();
            root.requestFocus();
            return true;
        } catch (RuntimeException ignored) {
            overlayFailed();
            return false;
        }
    }

    private void overlayFailed() {
        // Never swallow all remote keys without a visible/recoverable lock window.
        ChildLockState.error(this, "Box kh\u00f4ng cho ph\u1ee7 m\u00e0n h\u00ecnh kh\u00f3a");
        if (audio != null) audio.release(true);
        Toast.makeText(this, "Kh\u00f4ng t\u1ea1o \u0111\u01b0\u1ee3c m\u00e0n h\u00ecnh kh\u00f3a; \u0111\u00e3 h\u1ee7y kh\u00f3a", Toast.LENGTH_LONG).show();
    }

    private void showPin() {
        if (blackout == null || pinPad != null) return;
        pinPad = new ChildLockPinPad(this, true, this::queue, this::hidePin);
        blackout.addView(pinPad, new FrameLayout.LayoutParams(-1, -1));
        secret.reset();
    }

    private void hidePin() {
        if (blackout != null && pinPad != null) blackout.removeView(pinPad);
        pinPad = null;
        secret.reset();
        if (blackout != null) blackout.requestFocus();
    }

    private void removeBlackout() {
        if (blackout == null) return;
        try { windows.removeViewImmediate(blackout); } catch (RuntimeException ignored) { }
        blackout = null;
        pinPad = null;
        secret.reset();
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        String key = event.getDeviceId() + ":" + event.getKeyCode() + ":" + event.getDownTime();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Boolean previous = keyPairs.held(key);
            if (previous != null) return previous;
            boolean consumed = filterKey(event);
            keyPairs.remember(key, consumed);
            return consumed;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            Boolean previous = keyPairs.release(key);
            if (previous == null || !previous) return false;
            filterKey(event); // Sequence/UI side effects only for a press we began consuming.
            return true;
        }
        return false;
    }

    private boolean filterKey(KeyEvent event) {
        if (!initialised || blackout == null || !ChildLockState.locked(this)) return false;
        int code = event.getKeyCode();
        // Power/TV infrared/CEC are outside this feature; do not try to prevent the owner powering off.
        if (code == KeyEvent.KEYCODE_POWER || code == KeyEvent.KEYCODE_SLEEP || code == KeyEvent.KEYCODE_WAKEUP) return false;
        if (pinPad != null) {
            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_HOME) {
                if (event.getAction() == KeyEvent.ACTION_UP) hidePin();
                return true;
            }
            // Deliver directly to our keypad and consume the original, so even firmware that does not
            // give accessibility overlays ordinary key focus cannot send PIN digits into an underlying app.
            if (entryKey(code)) { pinPad.handleRemote(event); return true; }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            int family = code == KeyEvent.KEYCODE_VOLUME_MUTE || code == KeyEvent.KEYCODE_MUTE ? 2
                    : code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER ? 1 : 0;
            if (secret.released(family, SystemClock.elapsedRealtime())) showPin();
        }
        return true;
    }

    private static boolean entryKey(int code) {
        return code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_DEL
                || (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9)
                || (code >= KeyEvent.KEYCODE_NUMPAD_0 && code <= KeyEvent.KEYCODE_NUMPAD_9);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (initialised && ChildLockState.locked(this)) queue();
    }
    @Override public void onInterrupt() { }

    private void cleanUp() {
        if (cleaned) return;
        cleaned = true;
        stopWaiting();
        handler.removeCallbacks(tick);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(listener);
        removeBlackout();
        if (audio != null) {
            boolean enabledNow = enabled(this);
            boolean restore = !ChildLockState.locked(this) || !enabledNow;
            if (!enabledNow) ChildLockState.unlock(this);
            audio.release(restore);
        }
        initialised = false;
        keyPairs.clear();
        if (instance == this) instance = null;
    }
    @Override public boolean onUnbind(Intent intent) { cleanUp(); return super.onUnbind(intent); }
    @Override public void onDestroy() { cleanUp(); super.onDestroy(); }
}
