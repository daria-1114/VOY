package com.example.voy.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
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
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;
import com.example.voy.enums.TripItemType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    private static final String CHANNEL_ID  = "trip_capture_channel";
    private static final int    NOTIF_ID    = 101;
    private static final String TAG         = "TripForegroundService";
    private static final String MOCK_FOLDER = "/sdcard/Voy/MockTrip/";

    // Core
    private TripRepository  tripRepository;
    private ExecutorService executor;
    private Future<?>       scanLoopFuture;
    private volatile boolean running = false;

    // Trip state
    private String userId;
    private String tripId;
    private long   tripStartTimeMs;
    private long   lastScanDateAddedSec;

    // Helpers
    private TripLocationManager  locationManager;
    private MediaScanner     mediaScanner;
    private TripJsonWriter   tripJsonWriter;
    private SensorManager sensorManager;
    private SensorEventListener stepListener;
    private int stepCounterBaseline = -1;
    private long lastStepDayOffset = -1;
    private long lastDayCardOffset = -1;
    private final java.util.Set<String> writtenUris = new java.util.HashSet<>();
    private long lastGpsRequestTime = 0;
    private static final long GPS_INTERVAL_MS = TimeUnit.MINUTES.toMillis(90);

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        tripRepository  = new TripRepository(getApplicationContext());
        executor        = Executors.newSingleThreadExecutor();
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
                    getApplicationContext(), tripId, tripStartTimeMs);

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

            if (incomingUserId == null || incomingTripId == null || incomingStart <= 0) {
                stopSelf();
                return START_NOT_STICKY;
            }

            userId          = incomingUserId;
            tripId          = incomingTripId;
            tripStartTimeMs = incomingStart;

            TripCaptureStateStore.saveActive(this, userId, tripId, tripStartTimeMs);

            long startSec = tripStartTimeMs / 1000L;
            TripCaptureStateStore.State state = TripCaptureStateStore.load(this); // checking to see if there was already a media scan for this trip
            lastScanDateAddedSec = state.lastScanDateAddedSec > 0 //the time(sec) of the last scanned item
                    ? state.lastScanDateAddedSec : startSec;

            tripJsonWriter = new TripJsonWriter(
                    getApplicationContext(), tripId, tripStartTimeMs);

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
            tripJsonWriter.close(System.currentTimeMillis());
            tripJsonWriter = null;
        }
        if (sensorManager != null && stepListener != null) {
            sensorManager.unregisterListener(stepListener);
            sensorManager = null;
            stepListener = null;
        }
        stopCapture();
        locationManager.stop();
        executor.shutdownNow();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    // Real trip

    private void startMediaScanLoop() {
        if (running) return;
        running = true;

        mediaScanner = new MediaScanner(getApplicationContext(), scannedItem -> {
            Location loc = locationManager.getLastLocation();
            Double lat = null, lng = null;
            if (locationManager.hasPermission() && loc != null) {
                lat = loc.getLatitude();
                lng = loc.getLongitude();
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

            tripRepository.insertTripItem(item);

            if (tripJsonWriter != null && !writtenUris.contains(item.localUri)) {
                writtenUris.add(item.localUri);
                tripJsonWriter.append(item);
            }
        });

        scanLoopFuture = executor.submit(() -> {
            while (running) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long elapsedMs  = currentTime - tripStartTimeMs;
                    long dayOffset  = elapsedMs / TimeUnit.MINUTES.toMillis(2);

                    if (dayOffset != lastDayCardOffset) {
                        lastDayCardOffset = dayOffset;
                        locationManager.resetDailyDistance();

                        String dayLabel = "Day " + (dayOffset + 1);

                        TripItemEntity dayEntity = new TripItemEntity(
                                UUID.randomUUID().toString(),
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
                    if(currentTime - lastGpsRequestTime >= GPS_INTERVAL_MS){
                        Log.i(TAG, "GPS interval finished. requesting single update");
                        locationManager.requestSingleUpdate();
                        lastGpsRequestTime = currentTime;
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
                    saveGpsBasedSteps();
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
        sensorManager = (android.hardware.SensorManager)
                getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.d(TAG, "No SensorManager — steps will use GPS estimate");
            return;
        }

        android.hardware.Sensor stepSensor = sensorManager.getDefaultSensor(
                android.hardware.Sensor.TYPE_STEP_COUNTER);

        if (stepSensor != null) {
            stepListener = new android.hardware.SensorEventListener() {
                @Override
                public void onSensorChanged(android.hardware.SensorEvent event) {
                    int totalSteps = (int) event.values[0];
                    if (stepCounterBaseline < 0) {
                        stepCounterBaseline = totalSteps;
                        return;
                    }
                    checkAndSaveSteps(totalSteps - stepCounterBaseline, false);
                }
                @Override
                public void onAccuracyChanged(
                        android.hardware.Sensor sensor, int accuracy) {}
            };
            sensorManager.registerListener(
                    stepListener, stepSensor,
                    android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Step counter sensor registered");
        } else {
            Log.d(TAG, "No step sensor — steps will use GPS estimate");
        }
    }

    private void checkAndSaveSteps(int steps, boolean isGpsBased) {
        long elapsedMs = System.currentTimeMillis() - tripStartTimeMs;
        long dayOffset = elapsedMs / TimeUnit.MINUTES.toMillis(2);

        if (dayOffset == lastStepDayOffset) return;
        lastStepDayOffset = dayOffset;

        String dayLabel = "Day " + (dayOffset + 1);
        String title    = dayLabel + " — " + steps + " steps"
                + (isGpsBased ? " (estimated)" : "");
        String meta     = buildStepsMeta(steps, dayLabel);

        TripItemEntity stepsItem = new TripItemEntity(
                UUID.randomUUID().toString(),
                tripId, userId,
                TripItemType.STEPS,
                System.currentTimeMillis(),
                null, null, null, null,
                title, meta
        );
        tripRepository.insertTripItem(stepsItem);
        if (tripJsonWriter != null) tripJsonWriter.append(stepsItem);
        Log.d(TAG, "Saved steps for " + dayLabel + ": " + steps
                + (isGpsBased ? " (GPS estimated)" : " (sensor)"));
    }

    private void saveGpsBasedSteps() {
        if (stepListener != null) return;
        if (locationManager == null) return;
        int estimatedSteps = locationManager.getEstimatedSteps();
        Log.d(TAG, "saveGpsBasedSteps called, estimatedSteps=" + estimatedSteps);
        if (estimatedSteps <= 0) return;
        checkAndSaveSteps(estimatedSteps, true);
        locationManager.resetDailyDistance();
    }
    private void stopRealTrip() {
        if (tripJsonWriter != null) {
            tripJsonWriter.close(System.currentTimeMillis());
            tripJsonWriter = null;
        }
        TripCaptureStateStore.clear(this);
        if (sensorManager != null && stepListener != null) {
            sensorManager.unregisterListener(stepListener);
            sensorManager = null;
            stepListener = null;
        }
        // Save final steps on trip stop
        if (locationManager != null) {
            int steps = locationManager.getEstimatedSteps();
            if (steps > 0) {
                long elapsedMs = System.currentTimeMillis() - tripStartTimeMs;
                long dayOffset = elapsedMs / TimeUnit.MINUTES.toMillis(2);
                String dayLabel = "Day " + (dayOffset + 1);
                String title    = dayLabel + " — " + steps + " steps";
                TripItemEntity stepsItem = new TripItemEntity(
                        UUID.randomUUID().toString(),
                        tripId, userId,
                        TripItemType.STEPS,
                        System.currentTimeMillis(),
                        null, null, null, null,
                        title,
                        buildStepsMeta(steps, dayLabel)
                );
                tripRepository.insertTripItem(stepsItem);
                Log.d(TAG, "Saved final steps on trip stop: " + steps);
            }
        }
        stopCapture();
        locationManager.stop();
        stopForeground(true);
        stopSelf();
    }
    // Mock trip — reads res/raw/mock_trip.json, inserts into Room
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
                int        steps     = day.getInt("steps");
                JSONArray  items     = day.getJSONArray("items");

                long dayStartOffsetMs = TimeUnit.HOURS.toMillis(24L * (dayNumber - 1));
                long dayDurationMs    = TimeUnit.HOURS.toMillis(23);
                long intervalMs       = items.length() > 1
                        ? dayDurationMs / (items.length() - 1) : 0;
                long dayStartTimestampMs = simStartMs + dayStartOffsetMs;

                TripItemEntity dayEntity = new TripItemEntity(
                        UUID.randomUUID().toString(),
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

                    String filePath = MOCK_FOLDER + filename;
                    File mockFile = new File(filePath);
                    Uri mockUri = Uri.fromFile(mockFile);
                    String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : ".jpg";
                    String internalUriStirng  = MediaCloner.cloneToInternal(
                            getApplicationContext(),
                            mockUri,
                            extension);

                    String metadataJson = buildMockMetadata(
                            type, filename, landmark);

                    TripItemEntity entity = new TripItemEntity(
                            UUID.randomUUID().toString(),
                            simTripId,
                            simUserId,
                            type,
                            timestampMs,
                            internalUriStirng,
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
                        dayLabel + " — " + steps + " steps",
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

    private String buildStepsMeta(int steps, String dayLabel) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("steps", steps);
            obj.put("dayLabel", dayLabel);
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