package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/** Only a salted verifier is stored for fixed PINs. Existing installs retain time mode. */
final class ChildLockState {
    static final String PREFS = "personal_child_lock_v1";
    private ChildLockState() {}
    static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static int boot(Context context) {
        try { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1); }
        catch (RuntimeException ignored) { return -1; }
    }
    static boolean locked(Context context) { return prefs(context).getBoolean("locked", false); }
    static boolean timed(Context context) { return prefs(context).getBoolean("timed", false); }
    static long remaining(Context context) {
        SharedPreferences p = prefs(context);
        return ChildLockRules.remaining(p.getLong("start", 0), p.getLong("deadline", 0),
                p.getLong("deadline_elapsed", 0), p.getInt("boot", -1), boot(context),
                System.currentTimeMillis(), SystemClock.elapsedRealtime());
    }
    static boolean start(Context context, int minutes) {
        if (minutes < 1 || minutes > 180) return false;
        long duration = minutes * 60_000L;
        long now = System.currentTimeMillis();
        return prefs(context).edit().putBoolean("locked", false).putBoolean("timed", true)
                .putLong("start", now).putLong("deadline", now + duration)
                .putLong("deadline_elapsed", SystemClock.elapsedRealtime() + duration)
                .putInt("boot", boot(context)).remove("error").commit();
    }
    static boolean lock(Context context) {
        return prefs(context).edit().putBoolean("locked", true).putBoolean("timed", false).remove("error").commit();
    }
    static boolean cancelTimer(Context context) {
        return prefs(context).edit().putBoolean("timed", false).commit();
    }
    static boolean unlock(Context context) {
        return prefs(context).edit().putBoolean("locked", false).putBoolean("timed", false)
                .remove("attempts").remove("until_wall").remove("until_elapsed").remove("until_boot").commit();
    }
    static void error(Context context, String message) {
        prefs(context).edit().putBoolean("locked", false).putBoolean("timed", false).putString("error", message).commit();
    }
    static long cooldown(Context context) {
        SharedPreferences p = prefs(context);
        return ChildLockRules.cooldown(p.getLong("until_wall", 0), p.getLong("until_elapsed", 0),
                p.getInt("until_boot", -1), boot(context), System.currentTimeMillis(), SystemClock.elapsedRealtime());
    }
    enum Result { OPEN, WRONG, WAIT }
    static boolean fixedPin(Context context) { return "fixed".equals(prefs(context).getString("pin_mode", "time")); }
    static boolean setFixedPin(Context context, String pin) {
        if (!ChildLockCredential.valid(pin)) return false;
        try {
            String record = ChildLockCredential.create(pin);
            return prefs(context).edit().putString("pin_verifier", record).putString("pin_mode", "fixed").commit();
        } catch (IllegalStateException ignored) { return false; }
    }
    static boolean useTimePin(Context context) {
        return prefs(context).edit().putString("pin_mode", "time").remove("pin_verifier").commit();
    }
    static Result submit(Context context, String pin, boolean unlock) {
        if (cooldown(context) > 0) return Result.WAIT;
        boolean matches = fixedPin(context)
                ? ChildLockCredential.matches(pin, prefs(context).getString("pin_verifier", ""))
                : ChildLockRules.matches(pin, System.currentTimeMillis());
        if (matches) {
            boolean saved = unlock ? unlock(context) : prefs(context).edit().remove("attempts")
                    .remove("until_wall").remove("until_elapsed").remove("until_boot").commit();
            return saved ? Result.OPEN : Result.WRONG;
        }
        SharedPreferences p = prefs(context);
        int n = p.getInt("attempts", 0) + 1;
        SharedPreferences.Editor edit = p.edit();
        if (n >= 5) {
            edit.putInt("attempts", 0).putLong("until_wall", System.currentTimeMillis() + ChildLockRules.COOLDOWN_MS)
                    .putLong("until_elapsed", SystemClock.elapsedRealtime() + ChildLockRules.COOLDOWN_MS)
                    .putInt("until_boot", boot(context));
        } else edit.putInt("attempts", n);
        edit.commit();
        return n >= 5 ? Result.WAIT : Result.WRONG;
    }
}
