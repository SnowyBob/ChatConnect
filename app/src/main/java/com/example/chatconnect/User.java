package com.example.chatconnect;

import com.google.firebase.firestore.PropertyName;

public class User {
    private String username;
    private String email;
    private String uid;
    private String profileImageUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String uid) {
        this.username = username;
        this.uid = uid;
    }

    public User(String username, String email, String uid, String profileImageUrl) {
        this.username = username;
        this.email = email;
        this.uid = uid;
        this.profileImageUrl = profileImageUrl;
    }

    // Keep getName for backward compatibility
    public String getName() {
        return username;
    }

    public void setName(String name) {
        this.username = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
