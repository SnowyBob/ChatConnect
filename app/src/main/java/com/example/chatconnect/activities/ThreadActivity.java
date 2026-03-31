package com.example.chatconnect.activities;

import android.os.Bundle;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.Message;
import com.example.chatconnect.MessagesAdapter;
import com.example.chatconnect.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ThreadActivity extends AppCompatActivity {

    private String communityId;
    private String postId;
    private String parentAuthor;
    private String parentContent;
    private String currentUserId;
    private String currentUserName;

    private RecyclerView recyclerView;
    private MessagesAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText replyEdit;
    private View sendBtn;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

        communityId = getIntent().getStringExtra("community_id");
        postId = getIntent().getStringExtra("post_id");
        parentAuthor = getIntent().getStringExtra("author_name");
        parentContent = getIntent().getStringExtra("post_content");
        currentUserId = FirebaseAuth.getInstance().getUid();

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thread");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ((TextView) findViewById(R.id.parent_author)).setText(parentAuthor);
        TextView contentTextView = findViewById(R.id.parent_content);
        contentTextView.setText(parentContent);

        View parentScroll = findViewById(R.id.parent_post_scroll);
        TextView readMoreBtn = findViewById(R.id.btn_read_more);

        // Limit the height of the parent post area so it doesn't push the input off screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int maxHeight = (int) (displayMetrics.heightPixels * 0.4); // Max 40% of screen height

        parentScroll.post(() -> {
            if (parentScroll.getHeight() > maxHeight) {
                ViewGroup.LayoutParams params = parentScroll.getLayoutParams();
                params.height = maxHeight;
                parentScroll.setLayoutParams(params);
            }
        });

        // Check if content is truncated
        contentTextView.post(() -> {
            Layout layout = contentTextView.getLayout();
            if (layout != null) {
                int lines = layout.getLineCount();
                if (lines > 0) {
                    if (layout.getEllipsisCount(lines - 1) > 0) {
                        readMoreBtn.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        readMoreBtn.setOnClickListener(v -> {
            contentTextView.setMaxLines(Integer.MAX_VALUE);
            readMoreBtn.setVisibility(View.GONE);
            
            // Re-apply height constraint after expansion just in case
            parentScroll.post(() -> {
                ViewGroup.LayoutParams params = parentScroll.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                parentScroll.setLayoutParams(params);
                
                parentScroll.post(() -> {
                    if (parentScroll.getHeight() > maxHeight) {
                        params.height = maxHeight;
                        parentScroll.setLayoutParams(params);
                    }
                });
            });
        });

        recyclerView = findViewById(R.id.thread_recycler_view);
        replyEdit = findViewById(R.id.edit_thread_message);
        sendBtn = findViewById(R.id.btn_send_reply);

        adapter = new MessagesAdapter(messageList, currentUserId);
        adapter.setGroup(true); // Show names in threads
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchCurrentUserInfo();
        loadMessages();

        sendBtn.setOnClickListener(v -> sendReply());
    }

    private void fetchCurrentUserInfo() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUserName = doc.getString("username");
            }
        });
    }

    private void loadMessages() {
        db.collection("communities").document(communityId)
                .collection("posts").document(postId)
                .collection("replies").orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            messageList.add(doc.toObject(Message.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendReply() {
        String text = replyEdit.getText().toString().trim();
        if (text.isEmpty()) return;

        Message message = new Message(text, currentUserId, currentUserName, new Date());
        
        db.collection("communities").document(communityId)
                .collection("posts").document(postId)
                .collection("replies").add(message)
                .addOnSuccessListener(aVoid -> {
                    replyEdit.setText("");
                    // Increment reply count on parent post
                    db.collection("communities").document(communityId)
                            .collection("posts").document(postId)
                            .update("replyCount", com.google.firebase.firestore.FieldValue.increment(1));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
