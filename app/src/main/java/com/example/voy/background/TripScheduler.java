package com.example.voy.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TripScheduler {
    private static final String TAG ="TripScheduler";
    public static final String ACTION_START_PLANNED = "com.example.voy.action.START_PLANNED_TRIP";
    public static final String ACTION_STOP_PLANNED = "com.example.voy.action.STOP_PLANNED_TRIP";
    public static void scheduleTripActivation(Context context, String tripId, String userId, long startMs, long endMs, String tripTitle){
        Intent intentAlarm = new Intent(context, ServiceRestartReceiver.class);
        intentAlarm.setAction(ACTION_START_PLANNED);
        intentAlarm.putExtra("tripId", tripId);
        intentAlarm.putExtra("userId", userId);
        intentAlarm.putExtra("startTime", startMs);
        intentAlarm.putExtra("endTime", endMs);
        intentAlarm.putExtra("tripTitle", tripTitle);
        int requestCode = tripId.hashCode();// unique code using the trip id string's hashcode so multiple future vacations dont't overlap
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intentAlarm,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        setExactAlarm(context, startMs, pendingIntent);
    }
    public static void scheduleTripDeactivation(Context context, String tripId, String userId, long endMs) {
        Intent intentAlarm = new Intent(context, ServiceRestartReceiver.class);
        intentAlarm.setAction(ACTION_STOP_PLANNED);
        intentAlarm.putExtra("tripId", tripId);
        intentAlarm.putExtra("userId", userId);

        int requestCode = tripId.hashCode() + 1; // +1 prevents overwriting the start alarm
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        setExactAlarm(context, endMs, pendingIntent);
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
        cancelAlarm(context, pendingIntent);
    }

    private static void cancelAlarm(Context context, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private static void setExactAlarm(Context context, long startMs, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,startMs,pendingIntent);
            }else{
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, startMs,pendingIntent);
            }
        }
    }
}
