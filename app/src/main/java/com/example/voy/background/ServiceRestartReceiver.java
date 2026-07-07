package com.example.voy.background;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.voy.R;
import com.example.voy.activities.MainActivity;
import com.example.voy.data.repository.TripRepository;

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
            long endTime = intent.getLongExtra("endTime", -1);
            String tripTitle = intent.getStringExtra("tripTitle");
            Log.w(TAG, "Predefined vacation alarm clock triggered! Waking up background service for: " + tripId);

            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            mainActivityIntent.putExtra("START_SCHEDULED_TRIP_ID", tripId);
            mainActivityIntent.putExtra("START_SCHEDULED_USER_ID", userId);
            mainActivityIntent.putExtra("START_SCHEDULED_START_TIME", startTime);
            mainActivityIntent.putExtra("START_SCHEDULED_END_TIME", endTime);
            mainActivityIntent.putExtra("START_SCHEDULED_TRIP_TITLE", tripTitle);
            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, tripId.hashCode(), mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,"TRIP ALERTS", NotificationManager.IMPORTANCE_HIGH
                );
                notificationManager.createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_not_started_24)
                    .setContentTitle("Your trip is ready to start!")
                    .setContentText("Tap here to start capturing your vacation.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);
            notificationManager.notify(tripId.hashCode(), builder.build());
            return;
        }
        if (TripScheduler.ACTION_STOP_PLANNED.equals(action)) {
            String tripId = intent.getStringExtra("tripId");
            String userId = intent.getStringExtra("userId");

            Log.w(TAG, "Vacation end time reached! Auto-stopping trip: " + tripId);

            TripRepository tripRepository = new TripRepository(context);
            tripRepository.finishTrip(userId, tripId, System.currentTimeMillis());

            Intent stopIntent = new Intent(context, TripForegroundService.class);
            stopIntent.setAction(TripForegroundService.ACTION_STOP);
            context.startService(stopIntent);
            return;
        }
        // SYSTEM RECOVERY FLOW
        TripCaptureStateStore.State state = TripCaptureStateStore.load(context);
        if (!state.isValid()) return;

        Log.d(TAG, "Restarting TripForegroundService after system memory kill");
        TripCaptureStateStore.markNeedsResume(context, true);

        Intent serviceIntent = new Intent(context, TripForegroundService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}