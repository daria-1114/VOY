package com.example.voy.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TripScheduler {
    private static final String TAG ="TripScheduler";
    public static final String ACTION_START_PLANNED = "com.example.voy.action.START_PLANNED_TRIP";
    public static void scheduleTripActivation(Context context, String tripId, String userId, long startMs){
        Intent intentAlarm = new Intent(context, ServiceRestartReceiver.class);
        intentAlarm.setAction(ACTION_START_PLANNED);
        intentAlarm.putExtra("tripId", tripId);
        intentAlarm.putExtra("userId", userId);
        intentAlarm.putExtra("startTime", startMs);

        int requestCode = tripId.hashCode();// unique code using the trip id string's hashcode so multiple future vacations dont't overlap
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intentAlarm,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if
        (alarmManager != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Firing exact timers even if the CPU enters deep sleep doze modes
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startMs, pendingIntent);
            }else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, startMs, pendingIntent);
            }
        }
    }
    public static void cancelTripActivation(Context context, String tripId) {
        Intent intentAlarm = new Intent(context, ServiceRestartReceiver.class);
        intentAlarm.setAction(ACTION_START_PLANNED);
        intentAlarm.putExtra("tripId", tripId);

        int requestCode = tripId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intentAlarm,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
