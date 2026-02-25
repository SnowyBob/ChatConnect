package com.example.chatconnect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextView emailTextView;
    private Button saveButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            emailTextView = findViewById(R.id.profile_email);
            usernameEditText = findViewById(R.id.profile_username);
            saveButton = findViewById(R.id.btn_save_profile);

            emailTextView.setText(currentUser.getEmail());
            loadUserProfile();

            saveButton.setOnClickListener(v -> saveProfileChanges());
        } else {
            finish();
        }
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        usernameEditText.setText(username);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileChanges() {
        String newUsername = usernameEditText.getText().toString().trim();
        if (newUsername.isEmpty()) {
            usernameEditText.setError("Username cannot be empty");
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("username", newUsername);

        db.collection("users").document(currentUserId).update(userUpdates)
                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
