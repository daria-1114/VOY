package com.example.voy.network;

import com.example.voy.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;

public class GeminiManager {
    private static GeminiManager instance;
    private final GenerativeModelFutures modelFutures;

    private GeminiManager() {
        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-flash-latest",
                BuildConfig.GEMINI_API_KEY
        );
        this.modelFutures = GenerativeModelFutures.from(generativeModel);
    }
    public static synchronized GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }
    public GenerativeModelFutures getModel() {
        return modelFutures;
    }
}
