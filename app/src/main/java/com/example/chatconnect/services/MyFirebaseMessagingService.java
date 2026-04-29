package com.example.chatconnect.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.chatconnect.ChatActivity;
import com.example.chatconnect.R;
import com.example.chatconnect.utils.ChatState;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "chat_connect_messages";
    private static final String CHANNEL_NAME = "ChatConnect Messages";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Handle Data payload
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Handle Notification payload (if any, though data is preferred for custom handling)
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // If data payload is missing but notification exists, fallback
            if (remoteMessage.getData().isEmpty()) {
                showNotification("ChatConnect", remoteMessage.getNotification().getBody(), null, null);
            }
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String senderName = data.get("senderName");
        String messageText = data.get("messageText");
        String chatId = data.get("chatId");
        String chatName = data.get("chatName");

        // REQUIREMENT: Do not show notification if the user is already in THAT chat
        if (chatId != null && chatId.equals(ChatState.getActiveChatId())) {
            Log.d(TAG, "User is in the active chat. Suppressing notification.");
            return;
        }

        if (senderName != null && messageText != null && chatId != null) {
            showNotification(senderName, messageText, chatId, chatName);
        }
    }

    private static final Map<String, List<String>> chatMessages = new HashMap<>();

    private void showNotification(String senderName, String messageText, String chatId, String chatName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications for new messages");
                channel.enableLights(true);
                channel.setLightColor(0xFFA500);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(this, ChatActivity.class);
        if (chatId != null) {
            intent.putExtra("chat_id", chatId);
            intent.putExtra("chat_name", chatName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = chatId != null ? chatId.hashCode() : (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Track messages per chat
        String key = chatId != null ? chatId : "unknown";
        if (!chatMessages.containsKey(key)) {
            chatMessages.put(key, new ArrayList<>());
        }
        chatMessages.get(key).add(senderName + ": " + messageText);

        List<String> messages = chatMessages.get(key);
        int unreadCount = messages.size();
        String displayName = chatName != null ? chatName : senderName;

        NotificationCompat.Builder builder;

        if (unreadCount == 1) {
            // Single message — just show it
            builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(displayName)
                    .setContentText(senderName + ": " + messageText)
                    .setAutoCancel(true)
                    .setColor(0xFFA500)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setGroup(key)
                    .setContentIntent(pendingIntent);
        } else {
            // Multiple messages — show count + inbox style
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            // Show last 5 messages (newest at top)
            int start = Math.max(0, messages.size() - 5);
            for (int i = messages.size() - 1; i >= start; i--) {
                inboxStyle.addLine(messages.get(i));
            }
            inboxStyle.setSummaryText(unreadCount + " new messages");

            builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(displayName + " (" + unreadCount + " messages)")
                    .setContentText(senderName + ": " + messageText)
                    .setStyle(inboxStyle)
                    .setAutoCancel(true)
                    .setColor(0xFFA500)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setGroup(key)
                    .setNumber(unreadCount)
                    .setContentIntent(pendingIntent);
        }

        // Use same notification ID per chat so it updates in place
        int notificationId = key.hashCode();
        notificationManager.notify(notificationId, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update token", e));
        }
    }

    public static void clearMessages(String chatId) {
        if (chatId != null) {
            chatMessages.remove(chatId);
        }
    }

    public static void clearAll() {
        chatMessages.clear();
    }
}
