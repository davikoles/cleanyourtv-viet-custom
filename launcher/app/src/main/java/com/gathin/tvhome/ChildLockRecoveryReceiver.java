package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Manifest requires the sender's WRITE_SECURE_SETTINGS; reserved for local ADB recovery. */
public final class ChildLockRecoveryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!"com.gathin.tvhome.action.RECOVER_CHILD_LOCK".equals(intent.getAction())) return;
        ChildLockState.unlock(context);
        ChildLockAudio.restore(context);
        setResultData("Child lock cleared; box audio restore requested.");
    }
}
