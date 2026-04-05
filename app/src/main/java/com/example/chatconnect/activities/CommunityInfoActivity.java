package com.example.chatconnect.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.R;
import com.example.chatconnect.User;
import com.example.chatconnect.adapters.MemberAdapter;
import com.example.chatconnect.managers.CommunityManager;
import com.example.chatconnect.models.Community;
import com.example.chatconnect.models.Role;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityInfoActivity extends AppCompatActivity {

    private String communityId;
    private Community community;
    private String currentUserId;
    private FirebaseFirestore db;
    private CommunityManager communityManager;

    private TextInputEditText nameEdit, topicEdit, descEdit;
    private Button saveButton, leaveButton, deleteButton;
    private RecyclerView membersRecyclerView, bannedRecyclerView;
    private LinearLayout bannedSection;
    private MemberAdapter memberAdapter, bannedAdapter;
    
    private List<User> memberList = new ArrayList<>();
    private List<User> bannedList = new ArrayList<>();
    private Map<String, String> memberRoles = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_info);

        communityId = getIntent().getStringExtra("community_id");
        currentUserId = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();
        communityManager = CommunityManager.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Community Info");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        nameEdit = findViewById(R.id.edit_community_name);
        topicEdit = findViewById(R.id.edit_community_topic);
        descEdit = findViewById(R.id.edit_community_description);
        saveButton = findViewById(R.id.btn_save_info);
        leaveButton = findViewById(R.id.btn_leave_community);
        deleteButton = findViewById(R.id.btn_delete_community);
        
        membersRecyclerView = findViewById(R.id.members_recycler_view);
        bannedRecyclerView = findViewById(R.id.banned_recycler_view);
        bannedSection = findViewById(R.id.banned_members_section);

        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberAdapter(memberList, memberRoles, currentUserId, this::onMemberAction);
        membersRecyclerView.setAdapter(memberAdapter);

        bannedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Use memberRoles so it knows the current user's role (Owner) to show the action button
        bannedAdapter = new MemberAdapter(bannedList, memberRoles, currentUserId, this::onBannedMemberAction);
        bannedRecyclerView.setAdapter(bannedAdapter);

        loadCommunityData();
        saveButton.setOnClickListener(v -> saveCommunityInfo());
        leaveButton.setOnClickListener(v -> confirmLeave());
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadCommunityData() {
        db.collection("communities").document(communityId).addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                community = value.toObject(Community.class);
                if (community != null) {
                    community.setId(value.getId());
                    displayCommunityInfo();
                    loadMembers();
                    loadBannedMembers();
                }
            } else if (value != null && !value.exists()) {
                finish();
            }
        });
    }

    private void displayCommunityInfo() {
        nameEdit.setText(community.getName());
        topicEdit.setText(community.getTopic());
        descEdit.setText(community.getDescription());

        Role currentRole = community.getUserRole(currentUserId);
        boolean isOwner = currentRole == Role.OWNER;
        boolean isAdmin = currentRole == Role.ADMIN;
        boolean canEdit = isOwner || isAdmin;
        
        nameEdit.setEnabled(canEdit);
        topicEdit.setEnabled(canEdit);
        descEdit.setEnabled(canEdit);
        saveButton.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        
        deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        leaveButton.setVisibility(!isOwner ? View.VISIBLE : View.GONE);
        
        bannedSection.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    }

    private void loadMembers() {
        memberRoles = community.getMembers();
        if (memberRoles == null) memberRoles = new HashMap<>();
        
        if (community.getOwnerId() != null && !memberRoles.containsKey(community.getOwnerId())) {
            memberRoles.put(community.getOwnerId(), Role.OWNER.name());
        }
        
        memberAdapter.updateRoles(memberRoles);
        bannedAdapter.updateRoles(memberRoles); // Also update banned adapter roles
        
        List<String> userIds = new ArrayList<>(memberRoles.keySet());
        if (userIds.isEmpty()) return;

        db.collection("users").whereIn(FieldPath.documentId(), userIds).get().addOnSuccessListener(queryDocumentSnapshots -> {
            memberList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    if (user.getUid() == null) user.setUid(doc.getId());
                    memberList.add(user);
                }
            }
            
            User owner = null;
            for (User u : memberList) {
                if (u.getUid().equals(community.getOwnerId())) {
                    owner = u;
                    break;
                }
            }
            if (owner != null) {
                memberList.remove(owner);
                memberList.add(0, owner);
            }
            
            memberAdapter.notifyDataSetChanged();
        });
    }

    private void loadBannedMembers() {
        List<String> bannedIds = community.getBannedUsers();
        if (bannedIds == null || bannedIds.isEmpty()) {
            bannedList.clear();
            bannedAdapter.notifyDataSetChanged();
            return;
        }

        db.collection("users").whereIn(FieldPath.documentId(), bannedIds).get().addOnSuccessListener(queryDocumentSnapshots -> {
            bannedList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    if (user.getUid() == null) user.setUid(doc.getId());
                    bannedList.add(user);
                }
            }
            bannedAdapter.notifyDataSetChanged();
        });
    }

    private void confirmLeave() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Community")
                .setMessage("Are you sure you want to leave " + community.getName() + "?")
                .setPositiveButton("Leave", (dialog, which) -> leaveCommunity())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveCommunity() {
        db.collection("communities").document(communityId)
                .update("members." + currentUserId, com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Left community", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Community")
                .setMessage("This will permanently delete " + community.getName() + ". This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCommunity())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCommunity() {
        db.collection("communities").document(communityId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Community deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveCommunityInfo() {
        String name = nameEdit.getText().toString().trim();
        String topic = topicEdit.getText().toString().trim();
        String desc = descEdit.getText().toString().trim();

        if (name.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("topic", topic);
        updates.put("description", desc);

        db.collection("communities").document(communityId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Info updated", Toast.LENGTH_SHORT).show());
    }

    private void onMemberAction(User user) {
        Role myRole = community.getUserRole(currentUserId);
        Role targetRole = community.getUserRole(user.getUid());

        if (myRole != Role.OWNER && myRole != Role.ADMIN) return;
        if (user.getUid().equals(currentUserId)) return;
        if (targetRole == Role.OWNER) return;

        List<String> optionsList = new ArrayList<>();
        if (myRole == Role.OWNER) {
            optionsList.add(targetRole == Role.ADMIN ? "Demote to Member" : "Promote to Admin");
            optionsList.add("Kick Member");
            optionsList.add("Ban Member");
        } else {
            if (targetRole != Role.ADMIN) {
                optionsList.add("Kick Member");
            }
        }
        
        if (optionsList.isEmpty()) return;
        String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Manage " + user.getName())
                .setItems(options, (dialog, which) -> {
                    String selection = options[which];
                    if (selection.contains("Promote")) {
                        communityManager.updateMemberRole(communityId, user.getUid(), Role.ADMIN);
                    } else if (selection.contains("Demote")) {
                        communityManager.updateMemberRole(communityId, user.getUid(), Role.MEMBER);
                    } else if (selection.contains("Kick")) {
                        kickMember(user.getUid());
                    } else if (selection.contains("Ban")) {
                        banMember(user.getUid());
                    }
                }).show();
    }

    private void onBannedMemberAction(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Unban " + user.getName())
                .setMessage("Do you want to unban this user?")
                .setPositiveButton("Unban", (dialog, which) -> unbanMember(user.getUid()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void kickMember(String userId) {
        db.collection("communities").document(communityId)
                .update("members." + userId, com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Member kicked", Toast.LENGTH_SHORT).show());
    }

    private void banMember(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("members." + userId, FieldValue.delete());
        updates.put("bannedUsers", FieldValue.arrayUnion(userId));

        db.collection("communities").document(communityId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Member banned", Toast.LENGTH_SHORT).show());
    }

    private void unbanMember(String userId) {
        db.collection("communities").document(communityId)
                .update("bannedUsers", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Member unbanned", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
