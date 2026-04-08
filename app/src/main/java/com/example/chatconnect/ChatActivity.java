package com.example.chatconnect;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.services.AiService;
import com.example.chatconnect.services.MyFirebaseMessagingService;
import com.example.chatconnect.utils.ChatState;
import com.example.chatconnect.utils.FcmSender;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText messageEditText;
    private ImageView btnAiReply;
    private ProgressBar aiProgressBar;
    
    // Reply UI
    private RelativeLayout replyPreviewLayout;
    private TextView replyPreviewName, replyPreviewText;
    private ImageView btnCancelReply;
    private Message replyTargetMessage = null;

    private String chatId;
    private String chatName;
    private String currentUserId;
    private String currentUserName;
    private String currentUserProfileImageUrl;
    private boolean isGroup = false;
    private List<String> participants = new ArrayList<>();
    private FirebaseFirestore db;
    private AiService aiService;
    private ListenerRegistration chatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chat_id");
        chatName = getIntent().getStringExtra("chat_name");

        if (chatId == null) {
            Toast.makeText(this, "Error: Chat not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set active chat for notification suppression
        ChatState.setActiveChatId(chatId);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(chatName != null ? chatName : "Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setOnClickListener(v -> {
            if (isGroup) {
                Intent intent = new Intent(ChatActivity.this, GroupDetailsActivity.class);
                intent.putExtra("chat_id", chatId);
                startActivity(intent);
            }
        });

        db = FirebaseFirestore.getInstance();
        aiService = new AiService();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            fetchCurrentUserInfo();
        }

        checkChatStatus();
        
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MessagesAdapter(messageList, currentUserId);
        
        // Reply Listeners
        adapter.setOnReplyClickListener(message -> {
            replyTargetMessage = message;
            showReplyPreview(message);
        });

        adapter.setOnMessageNavigateListener(this::navigateToMessage);

        messagesRecyclerView.setAdapter(adapter);

        messageEditText = findViewById(R.id.message_input);
        ImageView sendButton = findViewById(R.id.send_button);
        btnAiReply = findViewById(R.id.btn_ai_reply);
        aiProgressBar = findViewById(R.id.ai_progress_bar);

        // Reply UI Initialization
        replyPreviewLayout = findViewById(R.id.reply_preview_layout);
        replyPreviewName = findViewById(R.id.reply_preview_name);
        replyPreviewText = findViewById(R.id.reply_preview_text);
        btnCancelReply = findViewById(R.id.btn_cancel_reply);

        btnCancelReply.setOnClickListener(v -> cancelReply());

        sendButton.setOnClickListener(v -> sendMessage());
        btnAiReply.setOnClickListener(v -> generateAiReply());

        loadMessages();
    }

    private void navigateToMessage(String messageId) {
        int position = adapter.getMessagePosition(messageId);
        if (position != -1) {
            messagesRecyclerView.scrollToPosition(position);
            adapter.highlightMessage(messageId);
        } else {
            Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReplyPreview(Message message) {
        replyPreviewName.setText(message.getSenderName());
        replyPreviewText.setText(message.getText());
        replyPreviewLayout.setVisibility(View.VISIBLE);
        messageEditText.requestFocus();
    }

    private void cancelReply() {
        replyTargetMessage = null;
        replyPreviewLayout.setVisibility(View.GONE);
    }

    private void fetchCurrentUserInfo() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("username");
                        currentUserProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                    }
                });
    }

    private void checkChatStatus() {
        chatListener = db.collection("chats").document(chatId).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) return;
            if (documentSnapshot != null && documentSnapshot.exists()) {
                isGroup = Boolean.TRUE.equals(documentSnapshot.getBoolean("isGroup"));
                participants = (List<String>) documentSnapshot.get("participants");
                adapter.setGroup(isGroup);
                
                String updatedName = documentSnapshot.getString("name");
                if (isGroup && updatedName != null && getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(updatedName);
                }

                // Auto-reset unread count if it's > 0 while we are in the chat
                Map<String, Long> unreadCounts = (Map<String, Long>) documentSnapshot.get("unreadCounts");
                if (unreadCounts != null && unreadCounts.containsKey(currentUserId)) {
                    long count = unreadCounts.get(currentUserId);
                    if (count > 0) {
                        resetUnreadCount();
                    }
                }
            }
        });
    }

    private void resetUnreadCount() {
        if (currentUserId == null || chatId == null) return;
        db.collection("chats").document(chatId)
                .update("unreadCounts." + currentUserId, 0);
    }

    private void generateAiReply() {
        if (messageList.isEmpty()) {
            Toast.makeText(this, "No messages to reply to", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiReply.setEnabled(false);
        aiProgressBar.setVisibility(View.VISIBLE);

        StringBuilder contextBuilder = new StringBuilder();
        int start = Math.max(0, messageList.size() - 10);
        for (int i = start; i < messageList.size(); i++) {
            Message m = messageList.get(i);
            contextBuilder.append(m.getSenderName()).append(": ").append(m.getText()).append("\n");
        }

        aiService.generateReply(currentUserName, contextBuilder.toString().trim(), new AiService.AiCallback() {
            @Override
            public void onGenerated(String text) {
                btnAiReply.setEnabled(true);
                aiProgressBar.setVisibility(View.GONE);
                messageEditText.setText(text);
                messageEditText.setSelection(text.length());
            }

            @Override
            public void onError(Exception e) {
                btnAiReply.setEnabled(true);
                aiProgressBar.setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this, "AI Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastMessage", messageText);
        updateData.put("timestamp", System.currentTimeMillis());
        
        // Increment unread counts for all other participants
        if (participants != null) {
            for (String participantId : participants) {
                if (!participantId.equals(currentUserId)) {
                    updateData.put("unreadCounts." + participantId, FieldValue.increment(1));

                    // Send Push Notification
                    db.collection("users").document(participantId).get().addOnSuccessListener(userDoc -> {
                        String token = userDoc.getString("fcmToken");
                        Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
                        boolean enabled = notificationsEnabled == null || notificationsEnabled; // default true

                        if (token != null && enabled) {
                            FcmSender.sendNotification(ChatActivity.this, token, currentUserName, messageText, chatId, chatName);
                        }
                    });
                }
            }
        }

        db.collection("chats").document(chatId).update(updateData);

        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("senderName", currentUserName != null ? currentUserName : "Unknown");
        message.put("senderProfileImageUrl", currentUserProfileImageUrl);
        message.put("text", messageText);
        message.put("timestamp", new Date());

        // Add reply metadata if active
        if (replyTargetMessage != null) {
            message.put("replyToMessageId", replyTargetMessage.getMessageId());
            message.put("replyToText", replyTargetMessage.getText());
            message.put("replyToSenderName", replyTargetMessage.getSenderName());
        }

        messagesRef.add(message).addOnSuccessListener(documentReference -> {
            cancelReply();
        });
        
        messageEditText.setText("");
    }

    private void loadMessages() {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshots != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Message message = doc.toObject(Message.class);
                            message.setMessageId(doc.getId());
                            messageList.add(message);
                        }
                        adapter.notifyDataSetChanged();
                        
                        // Handle initial navigation to a specific message (from Search or Reply)
                        String scrollToId = getIntent().getStringExtra("scroll_to_message_id");
                        if (scrollToId != null) {
                            messagesRecyclerView.post(() -> {
                                navigateToMessage(scrollToId);
                                getIntent().removeExtra("scroll_to_message_id");
                            });
                        } else if (!messageList.isEmpty()) {
                            messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChatState.setActiveChatId(chatId);
        resetUnreadCount();

        // Clear notification for this chat
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(chatId.hashCode());

        // Clear tracked messages in the service
        MyFirebaseMessagingService.clearMessages(chatId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatState.setActiveChatId(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            chatListener.remove();
        }
        ChatState.setActiveChatId(null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
