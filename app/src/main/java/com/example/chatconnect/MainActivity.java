package com.example.chatconnect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.activities.CommunityListActivity;
import com.example.chatconnect.activities.SearchActivity;
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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_LONG).show();
                }
            });

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
        ImageView searchButton = findViewById(R.id.search_button);

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

        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        adapter.setOnChatLongClickListener(chat -> {
            if (!chat.isGroup()) {
                showDeleteChatDialog(chat);
            }
        });

        askNotificationPermission();
        loadChats();
    }

    private void showDeleteChatDialog(Chat chat) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Delete chat with " + chat.getName() + "? Messages will be cleared on your side only.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("chats").document(chat.getId())
                            .update("deletedAt." + currentUserId, System.currentTimeMillis())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete chat", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
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
                            // Check if user "deleted" this chat
                            Map<String, Long> deletedAt = (Map<String, Long>) document.get("deletedAt");
                            Long userDeletedAt = (deletedAt != null) ? deletedAt.get(currentUserId) : null;
                            Long lastTimestamp = document.getLong("timestamp");

                            // If deleted and no new messages since, skip this chat
                            if (userDeletedAt != null && (lastTimestamp == null || lastTimestamp <= userDeletedAt)) {
                                continue;
                            }
                            String chatId = document.getId();
                            String lastMessage = document.getString("lastMessage");
                            if (lastMessage == null) lastMessage = "";
                            
                            boolean isGroup = Boolean.TRUE.equals(document.getBoolean("isGroup"));
                            String profileImageUrl = document.getString("profileImageUrl");
                            
                            // Load unread count for current user
                            int unreadCount = 0;
                            Map<String, Long> unreadCounts = (Map<String, Long>) document.get("unreadCounts");
                            if (unreadCounts != null && unreadCounts.containsKey(currentUserId)) {
                                unreadCount = unreadCounts.get(currentUserId).intValue();
                            }

                            if (isGroup) {
                                String groupName = document.getString("name");
                                Chat chat = new Chat(chatId, groupName != null ? groupName : "Group Chat", lastMessage, true, profileImageUrl);
                                chat.setUnreadCount(unreadCount);
                                chatsMap.put(chatId, chat);
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
                                        final String finalLastMessage = lastMessage;
                                        final int finalUnreadCount = unreadCount;
                                        db.collection("users").document(otherUserId).get()
                                                .addOnSuccessListener(userDoc -> {
                                                    String username = userDoc.getString("username");
                                                    String userProfileImageUrl = userDoc.getString("profileImageUrl");
                                                    if (username != null) {
                                                        Chat chat = new Chat(chatId, username, finalLastMessage, false, userProfileImageUrl);
                                                        chat.setUnreadCount(finalUnreadCount);
                                                        chatsMap.put(chatId, chat);
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
