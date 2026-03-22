package com.example.chatconnect.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.chatconnect.models.Community;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service to handle AI interactions using Google's Gemini API.
 */
public class AiService {

    private static final String TAG = "AiService";
    private static final String GEMINI_API_KEY = "AIzaSyDa4YLc2NYHVkFEWjkijErx_ouGji3Sn3w";

    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AiCallback {
        void onGenerated(String text);
        void onError(Exception e);
    }

    public AiService() {
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", GEMINI_API_KEY);
        model = GenerativeModelFutures.from(gm);
    }

    /**
     * Generates a high-quality post based on community topic, description, and user prompt using Gemini.
     */
    public void generatePost(Community community, String userPrompt, AiCallback callback) {
        if (community == null) {
            callback.onError(new Exception("Community data is missing"));
            return;
        }

        String topic = community.getTopic() != null ? community.getTopic() : "General Discussion";
        String desc = community.getDescription() != null ? community.getDescription() : "Our community";

        // Build a robust prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Write a natural, engaging, and high-quality post for a community feed.\n");
        promptBuilder.append("Community Topic: ").append(topic).append("\n");
        promptBuilder.append("Community Description: ").append(desc).append("\n");

        if (userPrompt != null && !userPrompt.trim().isEmpty()) {
            promptBuilder.append("Specific context for this post: ").append(userPrompt).append("\n");
        }

        promptBuilder.append("\nRequirements:\n");
        promptBuilder.append("- Output ONLY the post content.\n");
        promptBuilder.append("- Do not include titles, labels, or meta-talk like 'Here is your post'.\n");
        promptBuilder.append("- Make it sound like a human member of the community wrote it.\n");
        promptBuilder.append("- Be helpful and interesting.");

        Content content = new Content.Builder()
                .addText(promptBuilder.toString())
                .build();

        try {
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String text = result.getText();
                    if (text == null || text.trim().isEmpty()) {
                        mainHandler.post(() -> callback.onError(new Exception("AI returned empty content (possibly blocked by safety filters).")));
                    } else {
                        mainHandler.post(() -> callback.onGenerated(text.trim()));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini generation failed", t);
                    mainHandler.post(() -> callback.onError(new Exception("AI Generation failed: " + t.getMessage())));
                }
            }, executor);
        } catch (Exception e) {
            Log.e(TAG, "Error starting AI generation", e);
            callback.onError(e);
        }
    }
}
