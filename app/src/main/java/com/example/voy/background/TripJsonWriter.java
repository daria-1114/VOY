package com.example.voy.background;



import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.voy.data.entities.TripItemEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class TripJsonWriter {
    private static final String TAG = "TripJsonWriter";
    private static final String VOY_FOLDER = "Voy";
    private final File outputFile;
    private final JSONArray itemsArray;
    private final String tripId;
    private final long tripStartTimeMs;
    public TripJsonWriter(Context context, String tripId, long tripStartTimeMs) {
        this.itemsArray = new JSONArray();
        this.tripStartTimeMs = tripStartTimeMs;
        this.tripId = tripId;
        File voyDir = new File(context.getExternalFilesDir(null), VOY_FOLDER);        if (!voyDir.exists())voyDir.mkdir();
        if (!voyDir.exists()) voyDir.mkdirs();
        outputFile = new File(voyDir, "trip_"+ tripId+".json");
        Log.d(TAG, "JSON will be written to: " + outputFile.getAbsolutePath());
    }
    public synchronized  void append(TripItemEntity item){
        try{
            JSONObject obj = new JSONObject();
            obj.put("id", item.id);
            obj.put("type", item.type.name());
            obj.put("timestamp", item.timestamp);
            obj.put("localUri", item.localUri != null ? item.localUri : "");
            if(item.lat != null) obj.put("lat", item.lat);
            if(item.lng != null) obj.put("lng", item.lng);
            if(item.title != null)obj.put("landmark", item.title);
            if(item.metadataJson != null){
                obj.put("metadata", new JSONObject(item.metadataJson));
            }
            itemsArray.put(obj);
            flush();
        } catch (Exception e) {
            Log.e(TAG, "failed to append item to JSON", e);
        }
    }

    private void flush() {
        try{
            JSONObject root = buildRoot(false, -1);
            write(root);
        } catch (Exception e) {
            Log.e(TAG, "Failed to flush JSON", e);
        }
    }
    public synchronized void close(long tripEndTimeMs) {
        try {
            JSONObject root = buildRoot(true, tripEndTimeMs);
            write(root);
            Log.d(TAG, "Trip JSON finalised: " + outputFile.getAbsolutePath()
                    + " — " + itemsArray.length() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Failed to close JSON", e);
        }
    }

    private void write(JSONObject root) throws Exception {
        FileWriter writer = new FileWriter(outputFile, false);
        writer.write(root.toString(2));
        writer.flush();
        writer.close();
    }

    private JSONObject buildRoot(boolean includeEndTime, long endTimeMs) throws Exception {
        JSONObject root = new JSONObject();
        root.put("tripId", tripId);
        root.put("startTime", tripStartTimeMs);
        if (includeEndTime) root.put("endTime", endTimeMs);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("itemCount", itemsArray.length());
        root.put("items", itemsArray);
        return root;
    }

    public String getFilePath() {
        return outputFile.getAbsolutePath();
    }

}
