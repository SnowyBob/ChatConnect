package com.example.chatconnect;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView chatsRecyclerView;
    private TextView emptyChatsText;
    private ArrayList<Chat> chats = new ArrayList<>();
    private ChatsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        chatsRecyclerView = findViewById(R.id.chats_recycler_view);
        emptyChatsText = findViewById(R.id.empty_chats_text);
        Button newChatButton = findViewById(R.id.new_chat_button);
        ImageView settingsButton = findViewById(R.id.settings);
        ImageView profileButton = findViewById(R.id.profile);

        // Add some dummy chats for demonstration
        chats.add(new Chat("Jessica Thompson", "Hey, I was wondering if..."));
        chats.add(new Chat("Michael Lee", "Let's catch up soon!"));

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

        handleIntent(getIntent());
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("new_chat_user_name")) {
            String newChatUserName = intent.getStringExtra("new_chat_user_name");
            boolean chatExists = false;
            for (Chat chat : chats) {
                if (chat.getUserName().equals(newChatUserName)) {
                    chatExists = true;
                    break;
                }
            }
            if (!chatExists) {
                chats.add(new Chat(newChatUserName, "")); // Start with an empty last message
                adapter.notifyDataSetChanged();
                updateUI();
            }
            // Remove the extra so it's not processed again
            intent.removeExtra("new_chat_user_name");
        }
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
