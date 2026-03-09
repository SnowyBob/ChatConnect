package com.example.chatconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private String chatId;
    private String chatName;
    private String currentUserId;
    private String currentUserName;
    private boolean isGroup = false;
    private FirebaseFirestore db;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(chatName != null ? chatName : "Chat");
        }

        // Make toolbar title clickable for group details
        toolbar.setOnClickListener(v -> {
            if (isGroup) {
                Intent intent = new Intent(ChatActivity.this, GroupDetailsActivity.class);
                intent.putExtra("chat_id", chatId);
                startActivity(intent);
            }
        });

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            fetchCurrentUserName();
        }

        checkIfGroup();

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MessagesAdapter(messageList, currentUserId);
        messagesRecyclerView.setAdapter(adapter);

        messageEditText = findViewById(R.id.message_input);
        ImageView sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void fetchCurrentUserName() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("username");
                    }
                });
    }

    private void checkIfGroup() {
        db.collection("chats").document(chatId).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) return;
            if (documentSnapshot != null && documentSnapshot.exists()) {
                isGroup = Boolean.TRUE.equals(documentSnapshot.getBoolean("isGroup"));
                adapter.setGroup(isGroup);
                
                // Update title in case it was changed in GroupDetailsActivity
                String updatedName = documentSnapshot.getString("name");
                if (isGroup && updatedName != null && getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(updatedName);
                }
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
        db.collection("chats").document(chatId).update(updateData);

        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("senderName", currentUserName != null ? currentUserName : "Unknown");
        message.put("text", messageText);
        message.put("timestamp", new Date());

        messagesRef.add(message);
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
                            messageList.add(message);
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
