package com.example.voy.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class ServiceRestartReceiver extends BroadcastReceiver {

    private static final String TAG = "ServiceRestartReceiver";
    private static final String CHANNEL_ID = "trip_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();

        // 1. CATCH THE SCHEDULED TRIP ALARM TIMER
        if (TripScheduler.ACTION_START_PLANNED.equals(action)) {
            String tripId = intent.getStringExtra("tripId");
            String userId = intent.getStringExtra("userId");
            long startTime = intent.getLongExtra("startTime", System.currentTimeMillis());

            Log.w(TAG, "Predefined vacation alarm clock triggered! Waking up background service for: " + tripId);

            // Package the intent payload cleanly to boot up the recording service loop
            Intent serviceIntent = new Intent(context, TripForegroundService.class);
            serviceIntent.setAction(TripForegroundService.ACTION_START);
            serviceIntent.putExtra(TripForegroundService.EXTRA_USER_ID, userId);
            serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_ID, tripId);
            serviceIntent.putExtra(TripForegroundService.EXTRA_TRIP_START_TIME, startTime);
            serviceIntent.putExtra(TripForegroundService.EXTRA_IS_PREDEFINED, true);

            ContextCompat.startForegroundService(context, serviceIntent);
            return; // Exit early so it doesn't run regular crash-recovery logic below
        }

        // 2. YOUR ORIGINAL CRASH/LOW-MEMORY SYSTEM RECOVERY FLOW
        TripCaptureStateStore.State state = TripCaptureStateStore.load(context);
        if (!state.isValid()) return;

        Log.d(TAG, "Restarting TripForegroundService after system memory kill clearance");
        TripCaptureStateStore.markNeedsResume(context, true);

        Intent serviceIntent = new Intent(context, TripForegroundService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}