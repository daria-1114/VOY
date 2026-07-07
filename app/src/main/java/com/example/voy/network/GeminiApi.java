package com.example.voy.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiApi {
    private static final String TAG ="GeminiApi";
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    public interface OnResult {
        void onSuccess(JSONArray landmarks);
        void onError(String error);
    }
    public static void generateItinerary(String city, int totalDays, OnResult callback){
        try{
            GenerativeModelFutures model = GeminiManager.getInstance().getModel();
            String prompt = "Suggest 2 famous landmarks to visit per day for a " + totalDays
                    + "-day trip to " + city + ". "
                    + "Use each landmark's exact official, English name as it appears in OpenStreetMap, "
                    + "without a leading 'The' and without translating it. "
                    + "Return ONLY a valid JSON array of objects without any markdown formatting. "
                    + "Each object must have exactly two keys: 'name' (string) and 'dayNumber' (integer).";
            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            response.addListener(() ->{
                try {
                    String aiText = response.get().getText();
                    if (aiText != null) {
                        aiText = aiText.replace("```json", "").replace("```", "").trim();
                    }
                    JSONArray landmarksArray = new JSONArray(aiText);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(landmarksArray));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse JSON from AI: ", e);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                }
            }, EXECUTOR);
        } catch (Exception e) {
            Log.e(TAG, "Gemini Request Failed", e);
            new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));        }
    }
}
