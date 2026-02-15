package com.example.chatconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
    private String chatUserId;
    private String currentUserId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatUserId = getIntent().getStringExtra("chat_user_id");
        String chatUserName = getIntent().getStringExtra("chat_user_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(chatUserName);
        }

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MessagesAdapter(messageList, currentUserId);
        messagesRecyclerView.setAdapter(adapter);

        messageEditText = findViewById(R.id.message_input);
        ImageView sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        String chatId = getChatId();

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", messageText);
        chatData.put("lastMessageTime", new Date());
        chatData.put("participants", java.util.Arrays.asList(currentUserId, chatUserId));
        db.collection("chats").document(chatId).set(chatData, com.google.firebase.firestore.SetOptions.merge());

        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("text", messageText);
        message.put("timestamp", new Date());

        messagesRef.add(message);
        messageEditText.setText("");
    }

    private void loadMessages() {
        db.collection("chats").document(getChatId()).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    messageList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Message message = doc.toObject(Message.class);
                        messageList.add(message);
                    }
                    adapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                });
    }

    private String getChatId() {
        // Create a consistent chat ID between two users
        if (currentUserId.compareTo(chatUserId) > 0) {
            return currentUserId + "_" + chatUserId;
        } else {
            return chatUserId + "_" + currentUserId;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
