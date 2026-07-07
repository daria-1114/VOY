package com.example.voy.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        TripCaptureStateStore.State state = TripCaptureStateStore.load(context);
        if (!state.isValid()) return;
        TripCaptureStateStore.markNeedsResume(context, true);
    }
}