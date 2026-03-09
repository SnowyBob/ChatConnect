package com.example.chatconnect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailsActivity extends AppCompatActivity {

    private TextInputEditText groupNameEditText;
    private Button saveNameButton;
    private RecyclerView membersRecyclerView;
    private UsersAdapter adapter;
    private List<User> memberList = new ArrayList<>();
    private String chatId;
    private String currentUserId;
    private FirebaseFirestore db;

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
        membersRecyclerView = findViewById(R.id.members_recycler_view);

        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Use UsersAdapter in non-selection mode
        adapter = new UsersAdapter(memberList, new UsersAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // Do nothing for now when clicking a member in this view
            }

            @Override
            public void onSelectionChanged(int count) {
            }
        });
        membersRecyclerView.setAdapter(adapter);

        loadGroupDetails();

        saveNameButton.setOnClickListener(v -> updateGroupName());
    }

    private void loadGroupDetails() {
        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        groupNameEditText.setText(name);
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(name);
                        }

                        List<String> participants = (List<String>) documentSnapshot.get("participants");
                        if (participants != null) {
                            loadMembers(participants);
                        }
                    }
                });
    }

    private void loadMembers(List<String> participantIds) {
        memberList.clear();
        for (String uid : participantIds) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String username = userDoc.getString("username");
                            if (uid.equals(currentUserId)) {
                                username += " (You)";
                            }
                            memberList.add(new User(username, uid));
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void updateGroupName() {
        String newName = groupNameEditText.getText().toString().trim();
        if (newName.isEmpty()) {
            groupNameEditText.setError("Name cannot be empty");
            return;
        }

        db.collection("chats").document(chatId).update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Group name updated", Toast.LENGTH_SHORT).show();
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(newName);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
