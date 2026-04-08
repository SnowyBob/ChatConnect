package com.example.chatconnect.utils;

import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class FcmSender {

    private static final String TAG = "FcmSender";
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private static final String PROJECT_ID = "chatconnect-5ba61";

    private static String getAccessToken(Context context) {
        try {
            InputStream stream = context.getAssets().open("service-account.json");
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(stream)
                    .createScoped(Collections.singletonList(FCM_SCOPE));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            Log.e(TAG, "Error getting access token", e);
            return null;
        }
    }

    public static void sendNotification(Context context, String token, String senderName,
                                        String messageText, String chatId, String chatName) {
        new Thread(() -> {
            try {
                String accessToken = getAccessToken(context);
                if (accessToken == null) {
                    Log.e(TAG, "Access token is null");
                    return;
                }

                String fcmUrl = "https://fcm.googleapis.com/v1/projects/" + PROJECT_ID + "/messages:send";

                // Build the JSON payload
                JSONObject data = new JSONObject();
                data.put("senderName", senderName != null ? senderName : "Someone");
                data.put("messageText", messageText != null ? messageText : "");
                data.put("chatId", chatId != null ? chatId : "");
                data.put("chatName", chatName != null ? chatName : "");

                JSONObject messageObj = new JSONObject();
                messageObj.put("token", token);
                messageObj.put("data", data);

                JSONObject body = new JSONObject();
                body.put("message", messageObj);

                // Send the request
                URL url = new URL(fcmUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "FCM Response Code: " + responseCode);

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error sending notification", e);
            }
        }).start();
    }
}