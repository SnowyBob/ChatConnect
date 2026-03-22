package com.example.chatconnect.managers;

import com.example.chatconnect.models.Community;
import com.example.chatconnect.models.Post;
import com.example.chatconnect.models.Role;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Map;

/**
 * Handles Firestore operations for Communities and Posts.
 */
public class CommunityManager {
    private static CommunityManager instance;
    private final FirebaseFirestore db;

    private CommunityManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized CommunityManager getInstance() {
        if (instance == null) {
            instance = new CommunityManager();
        }
        return instance;
    }

    // --- Community Methods ---

    public Task<Void> createCommunity(Community community) {
        DocumentReference doc = db.collection("communities").document();
        community.setId(doc.getId());
        return doc.set(community);
    }

    public Query getAllCommunities() {
        return db.collection("communities");
    }

    public Task<Void> updateMemberRole(String communityId, String userId, Role role) {
        return db.collection("communities").document(communityId)
                .update("members." + userId, role.name());
    }

    // --- Permission Checking ---

    public boolean canPost(Community community, String userId) {
        Role role = community.getUserRole(userId);
        return role == Role.OWNER || role == Role.ADMIN;
    }

    public boolean canManageRoles(Community community, String userId) {
        return community.getUserRole(userId) == Role.OWNER;
    }

    // --- Post Methods ---

    public Task<DocumentReference> createPost(Post post) {
        return db.collection("communities").document(post.getCommunityId())
                .collection("posts").add(post);
    }

    public Query getPostsQuery(String communityId) {
        return db.collection("communities").document(communityId)
                .collection("posts").orderBy("timestamp", Query.Direction.DESCENDING);
    }
}
