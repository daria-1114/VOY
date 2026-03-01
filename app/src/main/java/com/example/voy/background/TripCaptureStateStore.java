package com.example.voy.background;

import android.content.Context;
import android.content.SharedPreferences;

public class TripCaptureStateStore {

    private static final String PREFS = "trip_capture_state";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_TRIP_ID = "tripId";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_LAST_SCAN_DATE_ADDED_SEC = "lastScanDateAddedSec";
    private static final String KEY_NEEDS_RESUME = "needsResume";
    public static void saveActive(Context ctx, String userId, String tripId, Long startTimeMs){
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_ACTIVE,true)
                .putString(KEY_USER_ID,userId)
                .putString(KEY_TRIP_ID,tripId)
                .putLong(KEY_START_TIME, startTimeMs)
                .apply();
    }
    public static void saveLastScanDateAddedSec(Context ctx, long sec) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(KEY_LAST_SCAN_DATE_ADDED_SEC, sec).apply();
    }
    public static void clear(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }
    public static State load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean active = sp.getBoolean(KEY_ACTIVE, false);
        String userId = sp.getString(KEY_USER_ID, null);
        String tripId = sp.getString(KEY_TRIP_ID, null);
        long start = sp.getLong(KEY_START_TIME, -1);
        long lastScan = sp.getLong(KEY_LAST_SCAN_DATE_ADDED_SEC, -1);
        return new State(active, userId, tripId, start, lastScan);
    }

    public static void markNeedsResume(Context context, boolean needs) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_NEEDS_RESUME, needs).apply();
    }
    public static boolean consumeNeedsResume(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean needs = sp.getBoolean(KEY_NEEDS_RESUME, false);
        if (needs) sp.edit().putBoolean(KEY_NEEDS_RESUME, false).apply();
        return needs;
    }
    public static class State {
        public final boolean active;
        public final String userId;
        public final String tripId;
        public final long startTimeMs;
        public final long lastScanDateAddedSec;
        public State(boolean active, String userId, String tripId, long startTimeMs, long lastScanDateAddedSec) {
            this.active = active;
            this.userId = userId;
            this.tripId = tripId;
            this.startTimeMs = startTimeMs;
            this.lastScanDateAddedSec = lastScanDateAddedSec;
        }
        public boolean isValid() {
            return active && userId != null && tripId != null && startTimeMs > 0;
        }
    }

}
