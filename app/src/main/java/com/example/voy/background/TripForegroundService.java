package com.example.voy.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.voy.R;
import com.example.voy.data.entities.LandmarkEntity;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;
import com.example.voy.enums.TripItemType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TripForegroundService extends Service {

    public static final String ACTION_START          = "com.example.voy.action.START_TRIP";
    public static final String ACTION_STOP           = "com.example.voy.action.STOP_TRIP";
    public static final String ACTION_START_MOCK     = "com.example.voy.action.START_MOCK_TRIP";
    public static final String EXTRA_USER_ID         = "extra_user_id";
    public static final String EXTRA_TRIP_ID         = "extra_trip_id";
    public static final String EXTRA_TRIP_START_TIME = "extra_trip_start_time";
    public static final String EXTRA_IS_PREDEFINED     = "extra_is_predefined";
    public static final String EXTRA_VACATION_END_TIME = "extra_vacation_end_time";
    public static final String EXTRA_TRIP_TITLE = "extra_trip_title";

    private static final String CHANNEL_ID  = "trip_capture_channel";
    private static final int    NOTIF_ID    = 101;
    private static final String TAG         = "TripForegroundService";
    private TripRepository  tripRepository;
    private ExecutorService executor;
    private Future<?>       scanLoopFuture;
    private volatile boolean running = false;
    private String userId;
    private String tripId;
    private long   tripStartTimeMs;
    private long   lastScanDateAddedSec;
    private TripLocationManager  locationManager;
    private MediaScanner     mediaScanner;
    private TripJsonWriter   tripJsonWriter;
    private SensorManager sensorManager;
    private SensorEventListener stepListener;
    private int stepCounterBaseline = -1;
    private long lastDayCardOffset = -1;
    private int lastSavedSteps = 0;
    private long lastStepDayOffset = -1;
    private int stepBaselineForDay = 0;
    private final java.util.Set<String> writtenUris = new java.util.HashSet<>();
    private long tripEndTimeMs = -1;
    private static final float VISITED_RADIUS_METERS = 200f;
    private ExecutorService proximityExecutor;
    @Override
    public void onCreate() {
        super.onCreate();
        tripRepository  = new TripRepository(getApplicationContext());
        executor        = Executors.newSingleThreadExecutor();
        proximityExecutor = Executors.newSingleThreadExecutor();
        locationManager = new TripLocationManager(getApplicationContext());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand action="
                + (intent == null ? "null" : intent.getAction()));

        if (intent == null || intent.getAction() == null) {
            TripCaptureStateStore.State state = TripCaptureStateStore.load(this);

            if (!state.isValid()) {
                stopSelf();
                return START_NOT_STICKY;
            }
            userId          = state.userId;
            tripId          = state.tripId;
            tripStartTimeMs = state.startTimeMs;
            long startSec   = tripStartTimeMs / 1000L;
            lastScanDateAddedSec = state.lastScanDateAddedSec > 0
                    ? state.lastScanDateAddedSec : startSec;

            tripJsonWriter = new TripJsonWriter(
                    getApplicationContext(), tripId);

            Log.d(TAG, "Resuming trip, tripId=" + tripId);
            startForeground(NOTIF_ID, buildNotification("Resuming trip capture…"));
            startMediaScanLoop();
            startStepCounting();
            return START_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            String incomingUserId = intent.getStringExtra(EXTRA_USER_ID);
            String incomingTripId = intent.getStringExtra(EXTRA_TRIP_ID);
            long   incomingStart  = intent.getLongExtra(EXTRA_TRIP_START_TIME, -1);
            boolean isPredefined = intent.getBooleanExtra(EXTRA_IS_PREDEFINED, false);
            tripEndTimeMs = intent.getLongExtra(EXTRA_VACATION_END_TIME, -1);
            if (incomingUserId == null || incomingTripId == null || incomingStart <= 0) {
                stopSelf();
                return START_NOT_STICKY;
            }

            userId          = incomingUserId;
            tripId          = incomingTripId;
            tripStartTimeMs = incomingStart;
            long elapsedMs = System.currentTimeMillis() - tripStartTimeMs;
            if (elapsedMs > 60_000) {
                // this is done to not print duplicate day cards if the service restarts
                lastDayCardOffset = elapsedMs / TimeUnit.HOURS.toMillis(24);
                lastStepDayOffset = lastDayCardOffset;
            } else {
                // It's a brand new trip and it starts normally
                lastDayCardOffset = -1;
                lastStepDayOffset = -1;
            }
            if (tripEndTimeMs > 0) {
                TripScheduler.scheduleTripDeactivation(getApplicationContext(), tripId, userId, tripEndTimeMs);
                long plannedMsElapsed = System.currentTimeMillis() - tripStartTimeMs;
                if(plannedMsElapsed <60_000){
                    executor.submit(() ->{
                        long durationMs = tripEndTimeMs - tripStartTimeMs;
                        int totalDays = (int) Math.ceil((double) durationMs/TimeUnit.HOURS.toMillis(24));
                        if(totalDays <= 0) totalDays = 1;
                        for(int i = 0; i < totalDays; i++){
                            int dayNumber = i+1;
                            String dayLabel = "Day " + dayNumber;
                            String dayId = "day_" + tripId + "_" + i;
                            long dayTimestamp = tripStartTimeMs + (i * TimeUnit.HOURS.toMillis(24));
                            TripItemEntity dayEntity = new TripItemEntity(
                                    dayId,
                                    tripId, userId,
                                    TripItemType.DAY,
                                    dayTimestamp,
                                    null, null, null, null,
                                    dayLabel,
                                    buildDayMeta(dayNumber, dayLabel)
                            );
                            tripRepository.insertTripItem(dayEntity);
                            if (tripJsonWriter != null) tripJsonWriter.append(dayEntity);
                            Log.d(TAG, "Pre-inserted DAY card for Planned Trip: " + dayLabel);
                        }
                    });
                }

            }
            if(isPredefined){
                String tripTitle = intent.getStringExtra(EXTRA_TRIP_TITLE);
                if (tripTitle == null || tripTitle.isEmpty()) {
                    tripTitle = "Active Vacation";
                }
                final String finalTitle = tripTitle;
                executor.submit(() ->{
                    tripRepository.activatePlannedTrip(userId, tripId, finalTitle);
                });
            }

            TripCaptureStateStore.saveActive(this, userId, tripId, tripStartTimeMs);

            long startSec = tripStartTimeMs / 1000L;
            TripCaptureStateStore.State state = TripCaptureStateStore.load(this); // checking to see if there was already a media scan for this trip
            lastScanDateAddedSec = state.lastScanDateAddedSec > 0 //the time(sec) of the last scanned item
                    ? state.lastScanDateAddedSec : startSec;

            tripJsonWriter = new TripJsonWriter(
                    getApplicationContext(), tripId);

            startForeground(NOTIF_ID, buildNotification("Capturing trip items…"));
            startMediaScanLoop();
            startStepCounting();
            return START_STICKY;
        }

        if (ACTION_STOP.equals(action)) {
            stopRealTrip();
            return START_NOT_STICKY;
        }

        if (ACTION_START_MOCK.equals(action)) {
            String incomingUserId = intent.getStringExtra(EXTRA_USER_ID);
            String incomingTripId = intent.getStringExtra(EXTRA_TRIP_ID);
            long   incomingStart  = intent.getLongExtra(EXTRA_TRIP_START_TIME, -1);

            if (incomingUserId == null || incomingTripId == null || incomingStart <= 0) {
                stopSelf();
                return START_NOT_STICKY;
            }

            userId          = incomingUserId;
            tripId          = incomingTripId;
            tripStartTimeMs = incomingStart;

            startForeground(NOTIF_ID, buildNotification("Loading mock trip…"));

            final String simUserId  = userId;
            final String simTripId  = tripId;
            final long   simStartMs = tripStartTimeMs;

            executor.submit(() -> {
                runMockTrip(simUserId, simTripId, simStartMs);
                tripRepository.finishTrip(simUserId, simTripId,
                        System.currentTimeMillis());
                stopForeground(true);
                stopSelf();
            });

            return START_NOT_STICKY;
        }

        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TripCaptureStateStore.State state = TripCaptureStateStore.load(this);
        if (state.isValid()) { // means the user didn't stop the trip(the system must have killed it)
            TripCaptureStateStore.markNeedsResume(this, true);
            Intent broadcast = new Intent(this, ServiceRestartReceiver.class);
            sendBroadcast(broadcast);
        }
        if (tripJsonWriter != null) {
            tripJsonWriter.close();
            tripJsonWriter = null;
        }
        if (sensorManager != null && stepListener != null) {
            sensorManager.unregisterListener(stepListener);
            sensorManager = null;
            stepListener = null;
        }
        stopCapture();
        locationManager.stop();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (proximityExecutor != null && !proximityExecutor.isShutdown()) {
            proximityExecutor.shutdownNow();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void startMediaScanLoop() {
        if (running) return;
        running = true;

        mediaScanner = new MediaScanner(getApplicationContext(), scannedItem -> {
            String originalUri = scannedItem.uri.toString();
            if (writtenUris.contains(originalUri)) return;
            if (tripRepository.hasMediaStoreId(tripId, scannedItem.mediaStoreId)){
                writtenUris.add(originalUri);
                return;
            }
            writtenUris.add(originalUri);
            locationManager.requestCurrentLocation(location ->{
                Double lat = null;
                Double lng = null;
                if(location !=null){
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
                String ext = scannedItem.type == TripItemType.VIDEO ? ".mp4" :
                        scannedItem.type == TripItemType.AUDIO ? ".mp3" : ".jpg";

                String internalUri = MediaCloner.cloneToInternal(
                        getApplicationContext(), scannedItem.uri, ext);

                TripItemEntity item = new TripItemEntity(
                        UUID.randomUUID().toString(),
                        tripId,
                        userId,
                        scannedItem.type,
                        scannedItem.timestampMs,
                        internalUri,
                        null,
                        lat,
                        lng,
                        null,
                        scannedItem.buildMetadataJson()
                );
                item.mediaStoreId = scannedItem.mediaStoreId;
                tripRepository.insertTripItem(item);

                if (tripJsonWriter != null) {
                    tripJsonWriter.append(item);
                }
                if (lat != null && lng != null) {
                    checkLandmarkProximity(lat, lng);
                }

                Log.d(TAG, "Saved item with fresh location");
            });
        });

        scanLoopFuture = executor.submit(() -> {
            while (running) {
                try {
                    if(tripEndTimeMs <= 0){
                        long currentTime = System.currentTimeMillis();
                        long elapsedMs  = currentTime - tripStartTimeMs;
                        long dayOffset  = elapsedMs / TimeUnit.HOURS.toMillis(24);

                        if (dayOffset != lastDayCardOffset) {
                            lastDayCardOffset = dayOffset;
                            String dayLabel = "Day " + (dayOffset + 1);
                            String dayId = "day_" + tripId + "_" + dayOffset;
                            TripItemEntity dayEntity = new TripItemEntity(
                                    dayId,
                                    tripId, userId,
                                    TripItemType.DAY,
                                    System.currentTimeMillis(),
                                    null, null, null, null,
                                    dayLabel,
                                    buildDayMeta((int) dayOffset + 1, dayLabel)
                            );
                            tripRepository.insertTripItem(dayEntity);
                            if (tripJsonWriter != null) tripJsonWriter.append(dayEntity);
                            Log.d(TAG, "Inserted DAY card for " + dayLabel);
                        }
                    }


                    if (mediaScanner.canScanAnything()) {
                        long tripStartSec = tripStartTimeMs / 1000L;
                        long sinceSec     = Math.max(
                                lastScanDateAddedSec - 2, tripStartSec); // -2 in case the item was added to the device's db later than it was taken
                        long maxSeen = mediaScanner.scan(sinceSec);
                        if (maxSeen > lastScanDateAddedSec) {
                            lastScanDateAddedSec = maxSeen;
                            TripCaptureStateStore.saveLastScanDateAddedSec(
                                    this, lastScanDateAddedSec);
                        }
                    }
                    Thread.sleep(90_000);
                } catch (InterruptedException e) {
                    running = false;
                } catch (Exception e) {
                    Log.e(TAG, "Scan loop error", e);
                }
            }
        });
    }

    private void stopCapture() {
        running = false;
        if (scanLoopFuture != null) {
            scanLoopFuture.cancel(true);
            scanLoopFuture = null;
        }
        writtenUris.clear();
    }

    private void startStepCounting() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.d(TAG, "No SensorManager — no steps updates");
            return;
        }

        Sensor stepSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_STEP_COUNTER);
        if (stepSensor == null) {
            Log.d(TAG, "No step sensor available");
            return;
        }
        stepListener = new SensorEventListener()  {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    int rawSensorTotal = (int) event.values[0];
                    long elapsedMs = System.currentTimeMillis() - tripStartTimeMs;
                    long dayOffset = elapsedMs/TimeUnit.HOURS.toMillis(24);
                    String dayLabel = "Day "+ (dayOffset + 1);
                    String stepsId = "steps_" + tripId + "_" + dayOffset;

                    tripRepository.upsertStepsForDay(stepsId, tripId, userId, dayLabel, System.currentTimeMillis(), rawSensorTotal);
                }
                @Override
                public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
            };
            sensorManager.registerListener(
                    stepListener, stepSensor,
                    android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Step counter sensor registered");

    }

    private void stopRealTrip() {
        if (tripJsonWriter != null) {
            tripJsonWriter.close();
            tripJsonWriter = null;
        }
        TripCaptureStateStore.clear(this);
        TripCaptureStateStore.markNeedsResume(this, false);
        if (sensorManager != null && stepListener != null) {
            sensorManager.unregisterListener(stepListener);
            sensorManager = null;
            stepListener = null;
        }
        executor.shutdownNow();
        proximityExecutor.shutdownNow();
        stopCapture();
        locationManager.stop();
        stopForeground(true);
        if (userId != null && tripId != null && executor != null && !executor.isShutdown()) {
            long finalEndTime = (tripEndTimeMs > 0) ? tripEndTimeMs : System.currentTimeMillis();

            executor.submit(() -> {
                try {
                    tripRepository.finishTrip(userId, tripId, finalEndTime);
                    Log.d(TAG, "Trip marked as finished in DB.");
                } finally {
                    executor.shutdown();
                    stopSelf();
                }
            });
        } else {
            stopSelf();
        }
    }

    private void runMockTrip(String simUserId, String simTripId, long simStartMs) {
        Log.d(TAG, "Mock trip starting for tripId=" + simTripId);
        try {
            String     json = readRawJson();
            JSONObject root = new JSONObject(json);
            JSONArray  days = root.getJSONArray("days");

            for (int d = 0; d < days.length(); d++) {
                JSONObject day       = days.getJSONObject(d);
                int        dayNumber = day.getInt("dayNumber");
                String     dayLabel  = day.getString("label");
                String dayId = "day_" + simTripId + "_" + (dayNumber - 1);
                int        steps     = day.getInt("steps");
                JSONArray  items     = day.getJSONArray("items");

                long dayStartOffsetMs = TimeUnit.HOURS.toMillis(24L * (dayNumber - 1));
                long dayDurationMs    = TimeUnit.HOURS.toMillis(23);
                long intervalMs       = items.length() > 1
                        ? dayDurationMs / (items.length() - 1) : 0;
                long dayStartTimestampMs = simStartMs + dayStartOffsetMs;

                TripItemEntity dayEntity = new TripItemEntity(
                        dayId,
                        simTripId,
                        simUserId,
                        TripItemType.DAY,
                        dayStartTimestampMs,
                        null, null, null, null,
                        dayLabel,
                        buildDayMeta(dayNumber, dayLabel)
                );
                tripRepository.insertTripItem(dayEntity);

                Log.d(TAG, "Mock inserted DAY card for " + dayLabel);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject itemObj  = items.getJSONObject(i);
                    String     filename = itemObj.getString("filename");
                    TripItemType type   = TripItemType.valueOf(
                            itemObj.getString("type"));
                    double lat          = itemObj.getDouble("lat");
                    double lng          = itemObj.getDouble("lng");
                    String landmark     = itemObj.optString("landmark", null);

                    long timestampMs = simStartMs + dayStartOffsetMs
                            + (intervalMs * i);

                    String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : ".jpeg";
                    File temporary = new File(getCacheDir(), filename);
                    try (InputStream inputStream = getAssets().open("mock_trip/" + filename);
                    OutputStream outputStream = new FileOutputStream(temporary)) {
                        byte[] buffer = new byte[8192];
                        int n;
                        while((n = inputStream.read(buffer)) != -1){
                            outputStream.write(buffer, 0, n);
                        }
                    }catch (Exception e){
                        Log.e(TAG, "Missing mock asset: " + filename, e);
                        continue;
                    }
                    Uri mockUri = Uri.fromFile(temporary);
                    String internalUriString = MediaCloner.cloneToInternal(getApplicationContext(), mockUri, extension);
                    temporary.delete();

                    String metadataJson = buildMockMetadata(
                            type, filename, landmark);

                    TripItemEntity entity = new TripItemEntity(
                            UUID.randomUUID().toString(),
                            simTripId,
                            simUserId,
                            type,
                            timestampMs,
                            internalUriString,
                            null,
                            lat,
                            lng,
                            landmark,
                            metadataJson
                    );
                    tripRepository.insertTripItem(entity);
                    Log.d(TAG, "Mock inserted " + type + " ["
                            + filename + "] @ "
                            + (landmark != null ? landmark : "no landmark"));

                }

                // Steps at end of each day
                long stepsTimestampMs = simStartMs + dayStartOffsetMs
                        + TimeUnit.HOURS.toMillis(23)
                        + TimeUnit.MINUTES.toMillis(59);

                TripItemEntity stepsEntity = new TripItemEntity(
                        UUID.randomUUID().toString(),
                        simTripId,
                        simUserId,
                        TripItemType.STEPS,
                        stepsTimestampMs,
                        null,
                        null,
                        null,
                        null,
                        dayLabel + " - " + steps + " steps",
                        buildStepsMeta(steps, dayLabel)
                );
                tripRepository.insertTripItem(stepsEntity);
                Log.d(TAG, "Mock inserted STEPS for " + dayLabel
                        + ": " + steps);
            }

            Log.d(TAG, "Mock trip complete");

        } catch (Exception e) {
            Log.e(TAG, "Mock trip failed", e);
        }
    }

    private String readRawJson() throws Exception {
        InputStream is     = getResources().openRawResource(R.raw.mock_trip);
        byte[]      buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    private String buildMockMetadata(TripItemType type, String filename,
                                     @Nullable String landmark) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", type.name());
            obj.put("filename", filename);
            if (landmark != null) obj.put("landmark", landmark);
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }
    private void checkLandmarkProximity(double photoLat, double photoLng) {
        proximityExecutor.submit(() -> {
            try {
                List<LandmarkEntity> unvisited =
                        tripRepository.getUnvisitedLandmarks(tripId);
                Log.d(TAG, "Proximity check — photo: " + photoLat + ", " + photoLng
                        + " | unvisited landmarks: " + (unvisited == null ? "null" : unvisited.size()));
                if (unvisited == null || unvisited.isEmpty()) return;

                for (LandmarkEntity landmark : unvisited) {
                    if (landmark.lat == null || landmark.lng == null) continue;
                    float[] result = new float[1];
                    Location.distanceBetween(
                            photoLat, photoLng,
                            landmark.lat, landmark.lng,
                            result);
                    Log.d(TAG, "Landmark: " + landmark.name
                            + " | stored: " + landmark.lat + ", " + landmark.lng
                            + " | distance: " + result[0] + "m"
                            + " | threshold: " + VISITED_RADIUS_METERS + "m");
                    if (result[0] <= VISITED_RADIUS_METERS) {
                        tripRepository.markLandmarkVisited(landmark.id);
                        Log.d(TAG, "Landmark visited: " + landmark.name
                                + " (" + result[0] + "m away)");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Proximity check failed", e);
            }
        });
    }
    private String buildStepsMeta(int steps, String dayLabel) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("steps", steps);
            obj.put("dayLabel", dayLabel);
            obj.put("baselineCounter", 0);
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }
    // Notification
    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_not_started_24)
                .setContentTitle("Voy — Trip capture")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }
    private String buildDayMeta(int dayNumber, String dayLabel) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("dayNumber", dayNumber);
            obj.put("dayLabel", dayLabel);
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Trip Capture",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}