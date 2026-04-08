package com.example.chatconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        SwitchMaterial notificationsSwitch = findViewById(R.id.switch_notifications);
        String userId = FirebaseAuth.getInstance().getUid();

        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        Boolean enabled = doc.getBoolean("notificationsEnabled");
                        notificationsSwitch.setChecked(enabled == null || enabled);

                        // Set listener AFTER loading so it doesn't trigger on load
                        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            String status = isChecked ? "enabled" : "disabled";
                            Toast.makeText(SettingsActivity.this, "Notifications " + status, Toast.LENGTH_SHORT).show();
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users").document(userId)
                                    .update("notificationsEnabled", isChecked);
                        });
                    });
        }

        TextView logoutButton = findViewById(R.id.setting_logout);
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
