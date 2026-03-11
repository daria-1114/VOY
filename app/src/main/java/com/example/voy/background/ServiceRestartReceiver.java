package com.example.voy.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class ServiceRestartReceiver extends BroadcastReceiver {

    private static final String TAG = "ServiceRestartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        TripCaptureStateStore.State state = TripCaptureStateStore.load(context);
        if (!state.isValid()) return;

        Log.d(TAG, "Restarting TripForegroundService after kill");
        TripCaptureStateStore.markNeedsResume(context, true);

        Intent serviceIntent = new Intent(context, TripForegroundService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}