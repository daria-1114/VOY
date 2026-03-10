package com.example.voy.background;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.voy.enums.TripItemType;

import org.json.JSONObject;

public class MediaScanner {
    private static final String TAG = "MediaScanner";
    private final Context context;
    private final OnItemFoundListener listener;
    public interface OnItemFoundListener{
        void onItemFound(ScannedItem item);
    }

    public static class ScannedItem {
        public final long mediaStoreId;
        public final Uri uri;
        public final long timestampMs;
        public final String mime;
        public final TripItemType type;
        public final long durationMs;

        public ScannedItem(long mediaStoreId, Uri uri, long timestampMs,
                           String mime, TripItemType type, long durationMs) {
            this.durationMs = durationMs;
            this.mediaStoreId = mediaStoreId;
            this.uri = uri;
            this.timestampMs = timestampMs;
            this.mime = mime;
            this.type = type;
        }
        public String buildMetadataJson(){
            try{
                JSONObject obj = new JSONObject();
                obj.put("mediaStoreId", mediaStoreId);
                obj.put("mime",mime!=null ? mime:"");
                obj.put("type", type!=null ? type: "");
                if(durationMs > 0 ) obj.put("durationMs", durationMs);
                return obj.toString();
            } catch (Exception e) {
                return null;
            }
        }

    }

    public MediaScanner(Context context, OnItemFoundListener listener) {
        this.context = context;
        this.listener = listener;
    }
    /**
     * Scans all media collections for items added after sinceSec.
     * Calls listener.onItemFound() for each new item found.
     * Returns the highest DATE_ADDED seen, so the service can
     * advance the scan window on the next pass.
     */
    public long scan(long sinceSec){
        long maxSeen = sinceSec;
        if(canReadImages()){
            maxSeen = Math.max(maxSeen, scanCollection(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    sinceSec, TripItemType.PHOTO, null));
        }
        if(canReadVideos()){
            maxSeen = Math.max(maxSeen, scanCollection(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    sinceSec, TripItemType.VIDEO, MediaStore.Video.VideoColumns.DURATION));
        }
        if(canReadAudio()){
            maxSeen = Math.max(maxSeen, scanCollection(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    sinceSec, TripItemType.AUDIO, MediaStore.Audio.AudioColumns.DURATION));
        }
        return maxSeen;
    }

    private long scanCollection(Uri collectionUri, long sinceSec,
                                  TripItemType type, @Nullable String durationColumn) {
        ContentResolver resolver = context.getContentResolver();
        java.util.ArrayList<String> projList = new java.util.ArrayList<>();
        projList.add(MediaStore.MediaColumns._ID);
        projList.add(MediaStore.MediaColumns.DATE_ADDED);
        projList.add(MediaStore.MediaColumns.MIME_TYPE);
        if (durationColumn != null) projList.add(durationColumn);

        String[] projection = projList.toArray(new String[0]);
        String   selection  = MediaStore.MediaColumns.DATE_ADDED + " > ?";
        String[] selArgs    = { String.valueOf(sinceSec) };
        String   sortOrder  = MediaStore.MediaColumns.DATE_ADDED + " ASC";
        long maxSeen = sinceSec;

        try (Cursor cursor = resolver.query(
                collectionUri, projection, selection, selArgs, sortOrder)) {
            if (cursor == null) return maxSeen;

            int idCol   = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            int durCol  = durationColumn != null
                    ? cursor.getColumnIndexOrThrow(durationColumn) : -1;

            while (cursor.moveToNext()) {
                long   mediaId      = cursor.getLong(idCol);
                long   dateAddedSec = cursor.getLong(dateCol);
                String mime         = cursor.getString(mimeCol);
                long   durationMs   = durCol != -1 ? cursor.getLong(durCol) : 0L;

                maxSeen = Math.max(maxSeen, dateAddedSec);

                Uri itemUri = Uri.withAppendedPath(
                        collectionUri, String.valueOf(mediaId));

                listener.onItemFound(new ScannedItem(
                        mediaId,
                        itemUri,
                        dateAddedSec * 1000L,
                        mime,
                        type,
                        durationMs
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "scanCollection failed for type=" + type, e);
        }

        return maxSeen;
    }

    private boolean canReadImages() {
        if (Build.VERSION.SDK_INT >= 33)
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canReadAudio() {
        if (Build.VERSION.SDK_INT >= 33)
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean canReadVideos() {
        if (Build.VERSION.SDK_INT >= 33)
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
    public boolean canScanAnything() {
        return canReadImages() || canReadVideos() || canReadAudio();
    }

}
