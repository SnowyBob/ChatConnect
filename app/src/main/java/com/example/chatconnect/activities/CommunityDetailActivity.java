package com.example.chatconnect.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.R;
import com.example.chatconnect.adapters.PostAdapter;
import com.example.chatconnect.managers.CommunityManager;
import com.example.chatconnect.models.Community;
import com.example.chatconnect.models.Post;
import com.example.chatconnect.models.Role;
import com.example.chatconnect.services.AiService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommunityDetailActivity extends AppCompatActivity {

    private String communityId;
    private Community currentCommunity;
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private CommunityManager communityManager;
    private String currentUserId;
    private String currentUserDisplayName;
    private String currentUserProfileImageUrl;

    private View postButtonsContainer;
    private Button joinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_detail);

        communityId = getIntent().getStringExtra("community_id");
        communityManager = CommunityManager.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getIntent().getStringExtra("community_name"));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Make the top part clickable to show info
        View topContainer = findViewById(R.id.top_info_container);
        if (topContainer != null) {
            topContainer.setOnClickListener(v -> openCommunityInfo());
        }
        toolbar.setOnClickListener(v -> openCommunityInfo());

        ((TextView) findViewById(R.id.detail_topic)).setText(getIntent().getStringExtra("community_topic"));
        ((TextView) findViewById(R.id.detail_description)).setText(getIntent().getStringExtra("community_description"));

        recyclerView = findViewById(R.id.posts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(postList);
        recyclerView.setAdapter(adapter);

        postButtonsContainer = findViewById(R.id.post_buttons_container);
        joinButton = findViewById(R.id.btn_join_community);

        findViewById(R.id.fab_create_post).setOnClickListener(v -> showCreatePostDialog());
        findViewById(R.id.fab_generate_ai).setOnClickListener(v -> showAiPromptDialog());
        joinButton.setOnClickListener(v -> joinCommunity());

        fetchCurrentUserInfo();
        loadCommunityAndPermissions();
        loadPosts();
    }

    private void openCommunityInfo() {
        Intent intent = new Intent(this, CommunityInfoActivity.class);
        intent.putExtra("community_id", communityId);
        startActivity(intent);
    }

    private void fetchCurrentUserInfo() {
        if (currentUserId == null) return;
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserDisplayName = documentSnapshot.getString("username");
                        currentUserProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (currentUserDisplayName == null || currentUserDisplayName.isEmpty()) {
                            currentUserDisplayName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                        }
                    }
                });
    }

    private void loadCommunityAndPermissions() {
        FirebaseFirestore.getInstance().collection("communities").document(communityId)
                .addSnapshotListener((value, error) -> {
                    if (value != null && value.exists()) {
                        currentCommunity = value.toObject(Community.class);
                        currentCommunity.setId(value.getId());
                        updateUIBasedOnRole();

                        // Check if kicked
                        if (currentCommunity.getMembers() != null && !currentCommunity.getMembers().containsKey(currentUserId)) {
                            Toast.makeText(this, "You are no longer a member of this community", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    private void updateUIBasedOnRole() {
        if (currentCommunity == null || currentUserId == null) return;

        Role role = currentCommunity.getUserRole(currentUserId);

        if (role == null) {
            joinButton.setVisibility(View.VISIBLE);
            postButtonsContainer.setVisibility(View.GONE);
        } else {
            joinButton.setVisibility(View.GONE);
            if (communityManager.canPost(currentCommunity, currentUserId)) {
                postButtonsContainer.setVisibility(View.VISIBLE);
            } else {
                postButtonsContainer.setVisibility(View.GONE);
            }
        }
    }

    private void joinCommunity() {
        communityManager.updateMemberRole(communityId, currentUserId, Role.MEMBER)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Joined Community!", Toast.LENGTH_SHORT).show());
    }

    private void loadPosts() {
        communityManager.getPostsQuery(communityId).addSnapshotListener((value, error) -> {
            if (error != null) return;
            postList.clear();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Post post = doc.toObject(Post.class);
                    post.setId(doc.getId());
                    postList.add(post);
                }
            }
            adapter.notifyDataSetChanged();
            if (!postList.isEmpty() && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                // Only scroll if we are at the top or idle to avoid jumping
            }
        });
    }

    private void showCreatePostDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null);
        TextInputEditText contentEdit = view.findViewById(R.id.edit_post_content);

        new AlertDialog.Builder(this)
                .setTitle("New Post")
                .setView(view)
                .setPositiveButton("Post", (dialog, which) -> {
                    String content = contentEdit.getText().toString().trim();
                    if (!content.isEmpty()) {
                        savePost(content, false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAiPromptDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null);
        TextInputEditText contentEdit = view.findViewById(R.id.edit_post_content);
        contentEdit.setHint("What should the AI write about?");

        new AlertDialog.Builder(this)
                .setTitle("Generate AI Post")
                .setView(view)
                .setPositiveButton("Generate", (dialog, which) -> {
                    generateAiPost(contentEdit.getText().toString().trim());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAiPost(String prompt) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Generating AI post...");
        progress.setCancelable(false);
        progress.show();

        new AiService().generatePost(currentCommunity, prompt, new AiService.AiCallback() {
            @Override
            public void onGenerated(String text) {
                progress.dismiss();
                savePost(text, true);
            }

            @Override
            public void onError(Exception e) {
                progress.dismiss();
                Toast.makeText(CommunityDetailActivity.this, "AI Generation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePost(String content, boolean isAi) {
        String nameToUse = currentUserDisplayName != null ? currentUserDisplayName : "Unknown User";
        Post post = new Post(communityId, currentUserId, nameToUse, content);
        post.setAiGenerated(isAi);
        post.setAuthorProfileImageUrl(currentUserProfileImageUrl);
        
        communityManager.createPost(post)
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.release();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
