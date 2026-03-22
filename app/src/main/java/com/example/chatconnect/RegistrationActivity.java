package com.example.chatconnect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.chatconnect.utils.SupabaseManager;
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

public class RegistrationActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private EditText usernameEditText, emailEditText, passwordEditText;
    private ImageView profileImageView;
    private Uri imageUri;
    private String currentPhotoPath;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SupabaseManager supabaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        supabaseManager = new SupabaseManager();

        usernameEditText = findViewById(R.id.username);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        profileImageView = findViewById(R.id.profile_image);
        Button createAccountButton = findViewById(R.id.create_account_button);
        TextView backToLogin = findViewById(R.id.back_to_login);

        profileImageView.setOnClickListener(v -> showImageSelectionDialog());

        createAccountButton.setOnClickListener(v -> registerNewUser());

        backToLogin.setOnClickListener(v -> finish());
    }

    private void showImageSelectionDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
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
                Toast.makeText(this, "Camera permission is required to use camera", Toast.LENGTH_SHORT).show();
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
        } catch (IOException ex) {
            Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
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
            } else if (requestCode == CAMERA_REQUEST) {
                File f = new File(currentPhotoPath);
                imageUri = Uri.fromFile(f);
                Glide.with(this).load(imageUri).circleCrop().into(profileImageView);
                profileImageView.setPadding(0, 0, 0, 0);
            }
        }
    }

    private void registerNewUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(RegistrationActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (imageUri != null) {
                                uploadToSupabaseAndSaveUser(user, username);
                            } else {
                                saveUserToFirestore(user, username, null);
                            }
                        }
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadToSupabaseAndSaveUser(FirebaseUser firebaseUser, String username) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        
        supabaseManager.uploadImage(this, imageUri, fileName, new SupabaseManager.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                saveUserToFirestore(firebaseUser, username, imageUrl);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RegistrationActivity.this, "Supabase Upload failed: " + message, Toast.LENGTH_SHORT).show();
                saveUserToFirestore(firebaseUser, username, null);
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String username, @Nullable String profileImageUrl) {
        String userId = firebaseUser.getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", firebaseUser.getEmail());
        user.put("uid", userId);
        if (profileImageUrl != null) {
            user.put("profileImageUrl", profileImageUrl);
        }

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegistrationActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegistrationActivity.this, "Error saving user data.", Toast.LENGTH_SHORT).show();
                });
    }
}
