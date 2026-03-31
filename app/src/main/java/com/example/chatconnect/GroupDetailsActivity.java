package com.example.chatconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailsActivity extends AppCompatActivity {

    private TextInputEditText groupNameEditText;
    private Button saveNameButton, leaveButton, deleteButton;
    private RecyclerView membersRecyclerView;
    private UsersAdapter adapter;
    private List<User> memberList = new ArrayList<>();
    private String chatId;
    private String currentUserId;
    private FirebaseFirestore db;
    private boolean isGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        chatId = getIntent().getStringExtra("chat_id");
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        groupNameEditText = findViewById(R.id.edit_group_name);
        saveNameButton = findViewById(R.id.btn_save_group_name);
        leaveButton = findViewById(R.id.btn_leave_group);
        deleteButton = findViewById(R.id.btn_delete_chat);
        membersRecyclerView = findViewById(R.id.members_recycler_view);

        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter(memberList, new UsersAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {}

            @Override
            public void onSelectionChanged(int count) {}
        });
        membersRecyclerView.setAdapter(adapter);

        loadGroupDetails();

        saveNameButton.setOnClickListener(v -> updateGroupName());
        leaveButton.setOnClickListener(v -> confirmLeave());
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadGroupDetails() {
        db.collection("chats").document(chatId).addSnapshotListener((documentSnapshot, e) -> {
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                isGroup = Boolean.TRUE.equals(documentSnapshot.getBoolean("isGroup"));
                
                groupNameEditText.setText(name);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(name != null ? name : "Details");
                }

                List<String> participants = (List<String>) documentSnapshot.get("participants");
                if (participants != null) {
                    loadMembers(participants);
                }

                // UI logic for Group vs Single Chat
                if (isGroup) {
                    leaveButton.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.GONE);
                    saveNameButton.setVisibility(View.VISIBLE);
                    groupNameEditText.setEnabled(true);
                } else {
                    leaveButton.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    saveNameButton.setVisibility(View.GONE);
                    groupNameEditText.setEnabled(false);
                }
            } else {
                finish();
            }
        });
    }

    private void loadMembers(List<String> participantIds) {
        memberList.clear();
        for (String uid : participantIds) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            User user = userDoc.toObject(User.class);
                            if (user != null) {
                                if (user.getUid() == null) user.setUid(uid);
                                memberList.add(user);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    private void confirmLeave() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveGroup() {
        db.collection("chats").document(chatId)
                .update("participants", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left group", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Are you sure you want to delete this chat?")
                .setPositiveButton("Delete", (dialog, which) -> deleteChat())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChat() {
        db.collection("chats").document(chatId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateGroupName() {
        String newName = groupNameEditText.getText().toString().trim();
        if (newName.isEmpty()) return;

        db.collection("chats").document(chatId).update("name", newName)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group name updated", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
