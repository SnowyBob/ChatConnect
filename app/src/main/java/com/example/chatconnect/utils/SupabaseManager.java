package com.example.chatconnect.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseManager {

    private static final String TAG = "SupabaseManager";

    private static final String SUPABASE_URL = "https://rbozihmfdhghulegivbi.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJib3ppaG1mZGhnaHVsZWdpdmJpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxOTEyMzMsImV4cCI6MjA4OTc2NzIzM30.u4W3X1GTnFDV1ZTOQvE91D5WaoNQAttyXi3nf_ouazw";
    private static final String BUCKET_NAME = "images";
    private static final String FOLDER_NAME = "profile_pics";

    private final OkHttpClient client;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String message);
    }

    public SupabaseManager() {
        this.client = new OkHttpClient();
    }

    public void uploadImage(Context context, Uri imageUri, String fileName, UploadCallback callback) {
        executor.execute(() -> {
            try {
                byte[] data = getBytes(context, imageUri);
                if (data == null || data.length == 0) {
                    mainHandler.post(() -> callback.onError("Failed to read image data: File is empty"));
                    return;
                }

                String storagePath = FOLDER_NAME + "/" + fileName;
                String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + storagePath;

                Log.d(TAG, "Uploading to: " + url);

                RequestBody requestBody = RequestBody.create(data, MediaType.parse("image/jpeg"));

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "Network failure: " + e.getMessage());
                        mainHandler.post(() -> callback.onError("Network failure: " + e.getMessage()));
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + storagePath;
                            Log.d(TAG, "Upload success: " + publicUrl);
                            mainHandler.post(() -> callback.onSuccess(publicUrl));
                        } else {
                            Log.e(TAG, "Upload failed (" + response.code() + "): " + responseBody);
                            mainHandler.post(() -> callback.onError("Supabase error " + response.code() + ": " + responseBody));
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in upload thread", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private byte[] getBytes(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        }
    }
}
