package com.example.chatconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewChatActivity extends AppCompatActivity {

    private static final String TAG = "NewChatActivity";
    private RecyclerView usersRecyclerView;
    private UsersAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    private View groupNameContainer;
    private TextInputEditText groupNameEditText;
    private Button createGroupButton;
    private ExtendedFloatingActionButton fabGroupChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        groupNameContainer = findViewById(R.id.group_name_container);
        groupNameEditText = findViewById(R.id.group_name_edit_text);
        createGroupButton = findViewById(R.id.btn_create_group);
        fabGroupChat = findViewById(R.id.fab_group_chat);

        usersRecyclerView = findViewById(R.id.users_recycler_view);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UsersAdapter(userList, new UsersAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                checkAndCreateSingleChat(user);
            }

            @Override
            public void onSelectionChanged(int count) {
                if (count > 0) {
                    fabGroupChat.setText("Cancel Group (" + count + ")");
                } else {
                    fabGroupChat.setText("Cancel Group");
                }
            }
        });
        usersRecyclerView.setAdapter(adapter);

        fabGroupChat.setOnClickListener(v -> toggleGroupMode());
        createGroupButton.setOnClickListener(v -> createGroupChat());

        loadUsers();
    }

    private void toggleGroupMode() {
        boolean isGroupMode = !adapter.isSelectionMode();
        adapter.setSelectionMode(isGroupMode);
        groupNameContainer.setVisibility(isGroupMode ? View.VISIBLE : View.GONE);
        fabGroupChat.setText(isGroupMode ? "Cancel Group" : "New Group");
        if (!isGroupMode) {
            groupNameEditText.setText("");
        }
    }

    private void checkAndCreateSingleChat(User user) {
        String chatId = getChatId(currentUserId, user.getUid());
        
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Chat already exists, just open it
                        openChat(chatId, user.getName());
                    } else {
                        // Create new chat
                        List<String> participants = new ArrayList<>();
                        participants.add(currentUserId);
                        participants.add(user.getUid());

                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("participants", participants);
                        chatData.put("lastMessage", "New chat started");
                        chatData.put("timestamp", System.currentTimeMillis());
                        chatData.put("isGroup", false);

                        db.collection("chats").document(chatId).set(chatData)
                                .addOnSuccessListener(aVoid -> openChat(chatId, user.getName()));
                    }
                });
    }

    private void openChat(String chatId, String chatName) {
        Intent intent = new Intent(NewChatActivity.this, ChatActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("chat_name", chatName);
        startActivity(intent);
        finish();
    }

    private void createGroupChat() {
        String groupName = groupNameEditText.getText().toString().trim();
        List<User> selectedUsers = adapter.getSelectedUsers();

        if (groupName.isEmpty()) {
            groupNameEditText.setError("Group name required");
            return;
        }

        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "Select at least one user", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        for (User user : selectedUsers) {
            participants.add(user.getUid());
        }

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("name", groupName);
        chatData.put("participants", participants);
        chatData.put("lastMessage", "Group created");
        chatData.put("timestamp", System.currentTimeMillis());
        chatData.put("isGroup", true);

        db.collection("chats").add(chatData)
                .addOnSuccessListener(documentReference -> openChat(documentReference.getId(), groupName));
    }

    private String getChatId(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void loadUsers() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String uid = document.getId();
                    String username = document.getString("username");
                    String profileImageUrl = document.getString("profileImageUrl");
                    if (username != null && !uid.equals(currentUserId)) {
                        User user = new User(username, uid);
                        user.setProfileImageUrl(profileImageUrl);
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
