package com.example.chatconnect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.chatconnect.utils.SupabaseManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private TextInputEditText usernameEditText;
    private TextView emailTextView;
    private ImageView profileImageView;
    private Button saveButton;
    private Uri imageUri;
    private String currentPhotoPath;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SupabaseManager supabaseManager;
    private String currentUserId;
    private String currentProfileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("currentPhotoPath");
            String savedUri = savedInstanceState.getString("imageUri");
            if (savedUri != null) imageUri = Uri.parse(savedUri);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        supabaseManager = new SupabaseManager();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            emailTextView = findViewById(R.id.profile_email);
            usernameEditText = findViewById(R.id.profile_username);
            profileImageView = findViewById(R.id.profile_image);
            saveButton = findViewById(R.id.btn_save_profile);

            emailTextView.setText(currentUser.getEmail());
            loadUserProfile();

            profileImageView.setOnClickListener(v -> showImageSelectionDialog());
            saveButton.setOnClickListener(v -> saveProfileChanges());
        } else {
            finish();
        }
    }

    private void showImageSelectionDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermission();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.chatconnect.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Camera could not be opened: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                Glide.with(this).load(imageUri).circleCrop().into(profileImageView);
                profileImageView.setPadding(0, 0, 0, 0);
            } else if (requestCode == CAMERA_REQUEST && currentPhotoPath != null) {
                File f = new File(currentPhotoPath);
                imageUri = Uri.fromFile(f);
                Glide.with(this).load(imageUri).circleCrop().into(profileImageView);
                profileImageView.setPadding(0, 0, 0, 0);
            }
        }
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                        usernameEditText.setText(username);

                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(profileImageView);
                            profileImageView.setPadding(0, 0, 0, 0);
                        }
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

        if (imageUri != null) {
            uploadImageAndUpdateProfile(newUsername);
        } else {
            updateFirestoreProfile(newUsername, currentProfileImageUrl);
        }
    }

    private void uploadImageAndUpdateProfile(String username) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        
        supabaseManager.uploadImage(this, imageUri, fileName, new SupabaseManager.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                updateFirestoreProfile(username, imageUrl);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestoreProfile(String username, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

        db.collection("users").document(currentUserId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    currentProfileImageUrl = imageUrl;
                    imageUri = null; // Clear selection after save
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoPath != null) outState.putString("currentPhotoPath", currentPhotoPath);
        if (imageUri != null) outState.putString("imageUri", imageUri.toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
