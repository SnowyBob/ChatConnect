package com.example.chatconnect.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.R;
import com.example.chatconnect.adapters.CommunityAdapter;
import com.example.chatconnect.managers.CommunityManager;
import com.example.chatconnect.models.Community;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommunityListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private List<Community> communityList = new ArrayList<>();
    private CommunityManager communityManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Communities");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        communityManager = CommunityManager.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.communities_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityAdapter(communityList, currentUserId, community -> {
            Intent intent = new Intent(this, CommunityDetailActivity.class);
            intent.putExtra("community_id", community.getId());
            intent.putExtra("community_name", community.getName());
            intent.putExtra("community_topic", community.getTopic());
            intent.putExtra("community_description", community.getDescription());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fab_create_community).setOnClickListener(v -> showCreateCommunityDialog());

        loadCommunities();
    }

    private void loadCommunities() {
        communityManager.getAllCommunities().addSnapshotListener((value, error) -> {
            if (error != null) return;
            communityList.clear();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Community community = doc.toObject(Community.class);
                    community.setId(doc.getId());
                    communityList.add(community);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showCreateCommunityDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_community, null);
        EditText nameEdit = view.findViewById(R.id.edit_community_name);
        EditText topicEdit = view.findViewById(R.id.edit_community_topic);
        EditText descEdit = view.findViewById(R.id.edit_community_description);

        new AlertDialog.Builder(this)
                .setTitle("Create Community")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String topic = topicEdit.getText().toString().trim();
                    String desc = descEdit.getText().toString().trim();
                    String userId = FirebaseAuth.getInstance().getUid();

                    if (!name.isEmpty() && userId != null) {
                        Community community = new Community(null, name, desc, topic, userId);
                        communityManager.createCommunity(community)
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Community Created!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create community", Toast.LENGTH_SHORT).show());
                    }
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
