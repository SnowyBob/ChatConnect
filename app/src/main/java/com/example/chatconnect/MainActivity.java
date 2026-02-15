package com.example.chatconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private RecyclerView chatsRecyclerView;
    private TextView emptyChatsText;
    private ArrayList<Chat> chats = new ArrayList<>();
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

        adapter = new ChatsAdapter(chats);
        chatsRecyclerView.setAdapter(adapter);

        updateUI();

        newChatButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewChatActivity.class));
        });

        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        loadChats();
    }

    private void loadChats() {
        Log.d("MainActivity", "Loading chats for user: " + currentUserId);

        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("MainActivity", "Query failed: " + e.getMessage());
                        return;
                    }

                    Log.d("MainActivity", "Found " + snapshots.size() + " chats");

                    chats.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
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
                                String lastMessage = document.getString("lastMessage");
                                String odId = otherUserId;

                                db.collection("users").document(otherUserId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            String username = userDoc.getString("username");
                                            if (username != null) {
                                                chats.add(new Chat(username, lastMessage != null ? lastMessage : "", odId));
                                                adapter.notifyDataSetChanged();
                                                updateUI();
                                            }
                                        });
                            }
                        }
                    }
                    updateUI();
                });
    }

    private void fetchUsernames(Set<String> userIds) {
        for (String userId : userIds) {
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                String username = documentSnapshot.getString("username");
                if (username != null) {
                    // For now, we don't have last message, so it's empty
                    chats.add(new Chat(username, "", userId));
                    adapter.notifyDataSetChanged();
                    updateUI();
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();
                    // Redirect to LoginActivity
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUI() {
        if (chats.isEmpty()) {
            chatsRecyclerView.setVisibility(View.GONE);
            emptyChatsText.setVisibility(View.VISIBLE);
        } else {
            chatsRecyclerView.setVisibility(View.VISIBLE);
            emptyChatsText.setVisibility(View.GONE);
        }
    }
}
