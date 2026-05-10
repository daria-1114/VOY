package com.example.voy.background;



import android.content.Context;
import android.util.Log;

import com.example.voy.data.entities.TripItemEntity;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class TripJsonWriter {
    private static final String TAG = "TripJsonWriter";
    private final File outputFile;
    private BufferedWriter writer;

    public TripJsonWriter(Context context, String tripId) {
        File tripsDirectory = new File(context.getFilesDir(),"trips");
        if(!tripsDirectory.exists())
            tripsDirectory.mkdir();
        outputFile = new File(tripsDirectory,"trip_"+tripId+".ndjson");
        try{
            writer = new BufferedWriter(
                    new FileWriter(outputFile, true)
            );
            Log.d(TAG, "Journal path: "+ outputFile.getAbsolutePath());
        }catch (Exception e){
            Log.e(TAG, "Failed opening journal",e);
        }
    }

    public synchronized  void append(TripItemEntity item){
        if(writer == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", item.id);
            obj.put("tripId", item.tripId);
            obj.put("userId", item.userId);
            obj.put("type", item.type.name());
            obj.put("timestamp", item.timestamp);
            if (item.localUri != null)
                obj.put("localUri", item.localUri);

            if (item.lat != null)
                obj.put("lat", item.lat);

            if (item.lng != null)
                obj.put("lng", item.lng);

            if (item.title != null)
                obj.put("title", item.title);

            if (item.metadataJson != null)
                obj.put("metadata", new JSONObject(item.metadataJson));
            writer.write(obj.toString());
            writer.newLine();

            writer.flush();
        }catch (Exception e) {
            Log.e(TAG, "Append failed", e);
        }
    }
    public synchronized void close() {

        try {

            if (writer != null) {

                writer.flush();
                writer.close();
            }

        } catch (Exception ignored) {}
    }
    public String getFilePath() {
        return outputFile.getAbsolutePath();
    }

}
