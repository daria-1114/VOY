package com.example.voy.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverpassApi {
    private static final String TAG = "OverpassAPI";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final long GAP_MS = 1000;
    private static final int MAX_RETRIES = 3;


    public interface OnResult {
        void onFound(double lat, double lng);
        void onNotFound();
    }

    public static void fetchCoordinates(String landmarkName, OnResult callback) {
        EXECUTOR.submit(()->{
            resolveName(landmarkName,callback);
            try {
                Thread.sleep(GAP_MS);
            }catch (InterruptedException ignored){}
        });
    }

    private static void resolveName(String landmarkName, OnResult callback) {
        for (int i = 0; i < MAX_RETRIES; i++){
            try {
                String url = "https://overpass-api.de/api/interpreter?data="+ URLEncoder.encode(buildQuery(landmarkName),"UTF-8");
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30_000);
                connection.setReadTimeout(30_000);
                int code = connection.getResponseCode();
                if(code == 429 || code == 504){
                    Thread.sleep( i * 2000L);
                    continue;
                }
                if(code != 200){
                    Log.e(TAG, "HTTP " + code + " for " + landmarkName + ": " + readStream(connection.getErrorStream()));
                    callback.onNotFound();
                    return;
                }
                JSONArray elements = new JSONObject(readStream(connection.getInputStream()))
                        .getJSONArray("elements");
                if(elements.length() == 0){
                    Log.w(TAG, "OSM found no matches for " + landmarkName);
                    callback.onNotFound();
                    return;
                }
                JSONObject best = null;
                for (int j = 0; j < elements.length(); j++){
                    JSONObject element = elements.getJSONObject(j);
                    JSONObject tags = element.optJSONObject("tags");
                    if (tags != null && tags.has("wikidata")) {
                        best = element; break;
                    }
                }
                if(best == null) best = elements.getJSONObject(0);
                double lat, lng;
                if (best.has("center")) {
                    lat = best.getJSONObject("center").getDouble("lat");
                    lng = best.getJSONObject("center").getDouble("lon");
                } else {
                    lat = best.getDouble("lat");
                    lng = best.getDouble("lon");
                }
                callback.onFound(lat, lng);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Attempt " + i + " failed for " + landmarkName, e);
                try { Thread.sleep((i+1) * 2000L); } catch (InterruptedException ignored) {}
            }
        }
        callback.onNotFound();
    }

    private static String readStream(InputStream stream) throws Exception {
        if( stream == null){
            return "";
        }
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[4096];
        int n;
        while((n = stream.read(buffer)) != -1){
            builder.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
        }
        stream.close();
        return builder.toString();
    }

    private static String buildQuery(String landmarkName) {
        return "[out:json][timeout:30];" +
                "(" +
                "  node[\"name:en\"=\"" + landmarkName + "\"][\"wikidata\"];" +
                "  way[\"name:en\"=\""  + landmarkName + "\"][\"wikidata\"];" +
                "  relation[\"name:en\"=\"" + landmarkName + "\"][\"wikidata\"];" +
                ");" +
                "out center 1;";
    }
}