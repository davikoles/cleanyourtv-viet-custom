package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;

/** Best-effort box audio control. Fixed-volume HDMI/CEC/passthrough must be tested on the real device. */
final class ChildLockAudio {
    private final Context context;
    private final AudioManager audio;
    private final AudioManager.OnAudioFocusChangeListener focus = change -> { };
    private boolean holdingFocus;
    private long lastPause;

    ChildLockAudio(Context context) {
        this.context = context;
        audio = context.getSystemService(AudioManager.class);
    }

    @SuppressWarnings("deprecation")
    boolean engage() {
        if (audio == null) return false;
        SharedPreferences prefs = ChildLockState.prefs(context);
        if (!prefs.getBoolean("audio_snapshot", false)) {
            try {
                boolean saved = prefs.edit().putBoolean("music_was_muted", audio.isStreamMute(AudioManager.STREAM_MUSIC))
                        .putBoolean("system_was_muted", audio.isStreamMute(AudioManager.STREAM_SYSTEM))
                        .putBoolean("audio_snapshot", true).commit();
                if (!saved) return false;
            } catch (RuntimeException ignored) { return false; }
        }
        if (!holdingFocus) {
            try {
                holdingFocus = audio.requestAudioFocus(focus, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            } catch (RuntimeException ignored) { }
        }
        maintain();
        return true;
    }

    void maintain() {
        if (audio == null || !ChildLockState.prefs(context).getBoolean("audio_snapshot", false)) return;
        // ADJUST_MUTE is idempotent: never use TOGGLE_MUTE while enforcing the blackout.
        mute(audio, AudioManager.STREAM_MUSIC, true);
        mute(audio, AudioManager.STREAM_SYSTEM, true);
        long now = SystemClock.elapsedRealtime();
        if (lastPause == 0 || now - lastPause >= 5000) {
            lastPause = now;
            try {
                audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
                audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
            } catch (RuntimeException ignored) { }
        }
    }

    @SuppressWarnings("deprecation")
    void release(boolean restore) {
        if (audio != null && holdingFocus) {
            try { audio.abandonAudioFocus(focus); } catch (RuntimeException ignored) { }
        }
        holdingFocus = false;
        lastPause = 0;
        if (restore) restore(context);
    }

    static void restore(Context context) {
        SharedPreferences prefs = ChildLockState.prefs(context);
        if (!prefs.getBoolean("audio_snapshot", false)) return;
        AudioManager audio = context.getSystemService(AudioManager.class);
        if (audio == null) return;
        mute(audio, AudioManager.STREAM_MUSIC, prefs.getBoolean("music_was_muted", false));
        mute(audio, AudioManager.STREAM_SYSTEM, prefs.getBoolean("system_was_muted", false));
        prefs.edit().remove("audio_snapshot").remove("music_was_muted").remove("system_was_muted").commit();
    }

    private static void mute(AudioManager audio, int stream, boolean muted) {
        try {
            if (audio.isStreamMute(stream) != muted) {
                audio.adjustStreamVolume(stream, muted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
            }
        } catch (RuntimeException ignored) { }
    }
}
