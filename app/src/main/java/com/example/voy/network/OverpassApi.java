package com.example.voy.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OverpassApi {
    private static final String TAG = "OverpassAPI";

    public interface OnResult {
        void onFound(double lat, double lng);
        void onNotFound();
    }

    public static void fetchCoordinates(String landmarkName, OnResult callback) {
        new Thread(() -> {
            try {
                String query =
                        "[out:json][timeout:10];" +
                                "(" +
                                "  node[\"name:en\"=\"" + landmarkName + "\"][\"wikidata\"];" +
                                "  way[\"name:en\"=\""  + landmarkName + "\"][\"wikidata\"];" +
                                "  relation[\"name:en\"=\"" + landmarkName + "\"][\"wikidata\"];" +
                                ");" +
                                "out center 1;";

                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr  = "https://overpass-api.de/api/interpreter?data=" + encoded;

                HttpURLConnection connection =
                        (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);

                InputStream is = connection.getInputStream();
                byte[] buffer = new byte[4096];
                StringBuilder sb = new StringBuilder();
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
                is.close();
                String response = sb.toString();

                JSONObject root     = new JSONObject(response);
                JSONArray  elements = root.getJSONArray("elements");

                if (elements.length() > 0) {
                    JSONObject best = null;
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject el = elements.getJSONObject(i);
                        JSONObject tags = el.optJSONObject("tags");
                        if (tags != null && tags.has("wikidata")) {
                            best = el;
                            break;
                        }
                    }
                    if (best == null) best = elements.getJSONObject(0);
                    double lat, lng;
                    if (best.has("center")) {
                        JSONObject center = best.getJSONObject("center");
                        lat = center.getDouble("lat");
                        lng = center.getDouble("lon");
                    } else {
                        lat = best.getDouble("lat");
                        lng = best.getDouble("lon");
                    }

                    callback.onFound(lat, lng);
                }

            } catch (Exception e) {
                Log.e(TAG, "Overpass query failed", e);
                callback.onNotFound();
            }
        }).start();
    }
}