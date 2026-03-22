package com.example.chatconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.activities.CommunityListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView chatsRecyclerView;
    private TextView emptyChatsText;
    private ArrayList<Chat> chatsList = new ArrayList<>();
    private Map<String, Chat> chatsMap = new LinkedHashMap<>();
    private ChatsAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        chatsRecyclerView = findViewById(R.id.chats_recycler_view);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        emptyChatsText = findViewById(R.id.empty_chats_text);
        Button newChatButton = findViewById(R.id.new_chat_button);
        ImageView settingsButton = findViewById(R.id.settings);
        ImageView profileButton = findViewById(R.id.profile);
        ImageView communitiesButton = findViewById(R.id.communities_button);

        adapter = new ChatsAdapter(chatsList);
        chatsRecyclerView.setAdapter(adapter);

        newChatButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewChatActivity.class));
        });

        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        communitiesButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CommunityListActivity.class));
        });

        loadChats();
    }

    private void loadChats() {
        if (currentUserId == null) return;
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("MainActivity", "Query failed: " + e.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        chatsMap.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            String chatId = document.getId();
                            String lastMessage = document.getString("lastMessage");
                            if (lastMessage == null) lastMessage = "";
                            
                            boolean isGroup = Boolean.TRUE.equals(document.getBoolean("isGroup"));

                            if (isGroup) {
                                String groupName = document.getString("name");
                                chatsMap.put(chatId, new Chat(chatId, groupName != null ? groupName : "Group Chat", lastMessage, true));
                                refreshAdapter();
                            } else {
                                ArrayList<String> participants = (ArrayList<String>) document.get("participants");
                                if (participants != null) {
                                    String otherUserId = null;
                                    for (String id : participants) {
                                        if (!id.equals(currentUserId)) {
                                            otherUserId = id;
                                            break;
                                        }
                                    }
                                    if (otherUserId != null) {
                                        final String finalOtherUserId = otherUserId;
                                        final String finalLastMessage = lastMessage;
                                        db.collection("users").document(otherUserId).get()
                                                .addOnSuccessListener(userDoc -> {
                                                    String username = userDoc.getString("username");
                                                    if (username != null) {
                                                        chatsMap.put(chatId, new Chat(chatId, username, finalLastMessage, false));
                                                        refreshAdapter();
                                                    }
                                                });
                                    }
                                }
                            }
                        }
                        refreshAdapter();
                    }
                });
    }

    private void refreshAdapter() {
        chatsList.clear();
        chatsList.addAll(chatsMap.values());
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void updateUI() {
        if (chatsList.isEmpty()) {
            chatsRecyclerView.setVisibility(View.GONE);
            emptyChatsText.setVisibility(View.VISIBLE);
        } else {
            chatsRecyclerView.setVisibility(View.VISIBLE);
            emptyChatsText.setVisibility(View.GONE);
        }
    }
}
