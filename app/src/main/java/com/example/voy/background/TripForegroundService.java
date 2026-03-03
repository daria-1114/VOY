package com.example.voy.background;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.voy.R;
import com.example.voy.data.entities.TripItemEntity;
import com.example.voy.data.repository.TripRepository;
import com.example.voy.enums.TripItemType;

import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TripForegroundService extends Service {

    public static final String ACTION_START = "com.example.voy.action.START_TRIP";
    public static final String ACTION_STOP  = "com.example.voy.action.STOP_TRIP";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_TRIP_ID = "extra_trip_id";
    public static final String EXTRA_TRIP_START_TIME = "extra_trip_start_time";
    private static final String CHANNEL_ID = "trip_capture_channel";
    private static final int NOTIF_ID = 101;
    private TripRepository tripRepository;
    private ExecutorService executor;
    private Future<?> scanLoopFuture;
    private volatile boolean running = false;
    private String userId;
    private String tripId;
    private long tripStartTimeMs;
    private long lastScanDateAddedSec;


    private static final String TAG = "TripForegroundService";

    @Override
    public void onCreate() {
        super.onCreate();
        tripRepository = new TripRepository(getApplicationContext());
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand action=" + (intent == null ? "null" : intent.getAction()));

        // when restarting the service- use store state for continuing what already started
        if (intent == null || intent.getAction() == null) {
            TripCaptureStateStore.State state = TripCaptureStateStore.load(this);
            if (!state.isValid()) {
                stopSelf();
                return START_NOT_STICKY;//after this the service doesn't restart, ONLY if the user presses start again
            }
            userId = state.userId;
            tripId = state.tripId;
            tripStartTimeMs = state.startTimeMs;
            long startSec = tripStartTimeMs / 1000L;
            lastScanDateAddedSec = (state.lastScanDateAddedSec > 0) ? state.lastScanDateAddedSec : startSec;
            Log.d(TAG, "Calling startForeground tripId=" + tripId);
            startForeground(NOTIF_ID, buildNotification("Resuming trip capture…"));
            startMediaScanLoop();
            return START_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            String incomingUserId = intent.getStringExtra(EXTRA_USER_ID);
            String incomingTripId = intent.getStringExtra(EXTRA_TRIP_ID);
            long incomingStart = intent.getLongExtra(EXTRA_TRIP_START_TIME, -1);

            if (incomingUserId == null || incomingTripId == null || incomingStart <= 0) {
                stopSelf();
                return START_NOT_STICKY;
            }

            userId = incomingUserId;
            tripId = incomingTripId;
            tripStartTimeMs = incomingStart;

            TripCaptureStateStore.saveActive(this, userId, tripId, tripStartTimeMs);

            TripCaptureStateStore.State state = TripCaptureStateStore.load(this);
            long startSec = tripStartTimeMs / 1000L;
            lastScanDateAddedSec = (state.lastScanDateAddedSec > 0) ? state.lastScanDateAddedSec : startSec;

            startForeground(NOTIF_ID, buildNotification("Capturing trip items…"));

            startMediaScanLoop();

            return START_STICKY;
        }
        if (ACTION_STOP.equals(action)) {
            TripCaptureStateStore.clear(this);
            stopCapture();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void startMediaScanLoop() {
        if (running) return;
        running = true;
        scanLoopFuture = executor.submit(() -> {
            while (running) {
                try {
                    // This method checks canReadImages/canReadVideos/canReadAudio internally
                    scanMediaStoreForNewItemsSafely();
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
    }

    private boolean canReadImages() {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canReadVideos() {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canReadAudio() {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
    private void scanMediaStoreForNewItemsSafely() {
        boolean any = canReadImages() || canReadVideos() || canReadAudio();
        if (!any) return;

        long tripStartSec = tripStartTimeMs / 1000L;
        long sinceSec = Math.max(lastScanDateAddedSec - 2, tripStartSec);

        long maxSeen = lastScanDateAddedSec;

        if (canReadImages()) {
            maxSeen = Math.max(maxSeen, scanCollectionSince(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    sinceSec,
                    TripItemType.PHOTO,
                    null
            ));
        }

        if (canReadVideos()) {
            maxSeen = Math.max(maxSeen, scanCollectionSince(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    sinceSec,
                    TripItemType.VIDEO,
                    MediaStore.Video.VideoColumns.DURATION
            ));
        }

        if (canReadAudio()) {
            maxSeen = Math.max(maxSeen, scanCollectionSince(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    sinceSec,
                    TripItemType.AUDIO,
                    MediaStore.Audio.AudioColumns.DURATION
            ));
        }

        if (maxSeen > lastScanDateAddedSec) {
            lastScanDateAddedSec = maxSeen;
            TripCaptureStateStore.saveLastScanDateAddedSec(this, lastScanDateAddedSec);
        }
    }


    private long scanCollectionSince(Uri collectionUri, long sinceSec, TripItemType type, @Nullable String durationColumn) {
        ContentResolver resolver = getContentResolver();

        java.util.ArrayList<String> projectionList = new java.util.ArrayList<>();
        projectionList.add(MediaStore.MediaColumns._ID);
        projectionList.add(MediaStore.MediaColumns.DATE_ADDED);
        projectionList.add(MediaStore.MediaColumns.MIME_TYPE);
        if (durationColumn != null) projectionList.add(durationColumn);

        String[] projection = projectionList.toArray(new String[0]);

        String selection = MediaStore.MediaColumns.DATE_ADDED + " > ?";
        String[] selectionArgs = new String[]{ String.valueOf(sinceSec) };
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " ASC";

        long maxSeen = sinceSec;

        try (Cursor cursor = resolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor == null) return maxSeen;

            int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            int durCol = (durationColumn != null) ? cursor.getColumnIndexOrThrow(durationColumn) : -1;

            while (cursor.moveToNext()) {
                long mediaId = cursor.getLong(idCol);
                long dateAddedSec = cursor.getLong(dateAddedCol);
                String mime = cursor.getString(mimeCol);
                long durationMs = (durCol != -1) ? cursor.getLong(durCol) : 0L;

                maxSeen = Math.max(maxSeen, dateAddedSec);

                Uri itemUri = Uri.withAppendedPath(collectionUri, String.valueOf(mediaId));
                long timestampMs = dateAddedSec * 1000L;

                String metadataJson = buildMediaMetadata(mediaId, mime, type, durationMs);

                TripItemEntity item = new TripItemEntity(
                        UUID.randomUUID().toString(),
                        tripId,
                        userId,
                        type,
                        timestampMs,
                        itemUri.toString(),
                        null,
                        null,
                        null,
                        null,
                        metadataJson
                );

                tripRepository.insertTripItem(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "scanCollectionSince failed for type=" + type, e);
        }

        return maxSeen;
    }

    private String buildMediaMetadata(long mediaStoreId, String mime, TripItemType type, long durationMs) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("mediaStoreId", mediaStoreId);
            obj.put("mime", mime);
            obj.put("type", type != null ? type.name() : null);
            if (durationMs > 0) obj.put("durationMs", durationMs);
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_not_started_24)
                .setContentTitle("Voy — Trip capture")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Trip Capture",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        stopCapture();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}