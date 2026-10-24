package com.gathin.tvhome;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/** Pure rules, kept separate so date boundaries, timers and the secret sequence can be tested. */
final class ChildLockRules {
    static final long COOLDOWN_MS = 60_000L;
    private static final DateTimeFormatter PIN = DateTimeFormatter.ofPattern("ddMMHH", Locale.ROOT)
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private ChildLockRules() {}

    // This is a predictable time code, not a cryptographic OTP or a parent-specific secret.
    static String pin(long wallTime) { return PIN.format(Instant.ofEpochMilli(wallTime)); }

    static boolean matches(String input, long wallTime) {
        if (input == null || input.length() != 6) return false;
        for (int i = 0; i < input.length(); i++) if (input.charAt(i) < '0' || input.charAt(i) > '9') return false;
        return pin(wallTime).equals(input);
    }

    static long remaining(long startWall, long deadlineWall, long deadlineElapsed, int savedBoot,
                          int boot, long wall, long elapsed) {
        if (deadlineWall <= startWall || deadlineElapsed <= 0) return 0;
        if (savedBoot >= 0 && savedBoot == boot) return Math.max(0, deadlineElapsed - elapsed);
        // Across reboot use the saved civil deadline; a rolled-back clock must not extend the allowance.
        if (wall < startWall - 60_000L) return 0;
        return Math.max(0, Math.min(deadlineWall - startWall, deadlineWall - wall));
    }

    static long cooldown(long untilWall, long untilElapsed, int savedBoot, int boot, long wall, long elapsed) {
        long left = savedBoot >= 0 && savedBoot == boot ? untilElapsed - elapsed : untilWall - wall;
        return Math.max(0, Math.min(COOLDOWN_MS, left));
    }

    static final class Secret {
        private int family, count;
        private long first, last;
        void reset() { family = 0; count = 0; first = 0; last = 0; }
        // family 1 = OK/Enter, 2 = Mute. Called once on release, never for held-key repeats.
        boolean released(int keyFamily, long elapsed) {
            if (keyFamily == 0) { reset(); return false; }
            if (keyFamily != family || count == 0 || elapsed < last
                    || elapsed - last > 1200 || elapsed - first > 3500) {
                family = keyFamily;
                count = 1;
                first = elapsed;
            } else count++;
            last = elapsed;
            if (count < 5) return false;
            reset();
            return true;
        }
    }

    /** Preserve each key's down/up filtering decision if the timer expires between those events. */
    static final class KeyPairs {
        private final Map<String, Boolean> held = new HashMap<>();
        Boolean held(String key) { return held.get(key); }
        void remember(String key, boolean consumed) {
            if (held.size() >= 128) held.clear();
            held.put(key, consumed);
        }
        Boolean release(String key) { return held.remove(key); }
        void clear() { held.clear(); }
    }
}
