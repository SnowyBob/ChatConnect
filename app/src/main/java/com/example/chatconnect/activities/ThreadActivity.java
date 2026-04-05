package com.example.chatconnect.activities;

import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.Message;
import com.example.chatconnect.MessagesAdapter;
import com.example.chatconnect.R;
import com.example.chatconnect.services.AiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ImageView btnAiReply;
    private ProgressBar aiProgressBar;
    
    // Reply UI
    private RelativeLayout replyPreviewLayout;
    private TextView replyPreviewName, replyPreviewText;
    private ImageView btnCancelReply;
    private Message replyTargetMessage = null;

    private FirebaseFirestore db;
    private AiService aiService;

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
        aiService = new AiService();

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

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int maxHeight = (int) (displayMetrics.heightPixels * 0.4);

        parentScroll.post(() -> {
            if (parentScroll.getHeight() > maxHeight) {
                ViewGroup.LayoutParams params = parentScroll.getLayoutParams();
                params.height = maxHeight;
                parentScroll.setLayoutParams(params);
            }
        });

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
        btnAiReply = findViewById(R.id.btn_ai_reply);
        aiProgressBar = findViewById(R.id.ai_progress_bar);

        // Reply UI Initialization
        replyPreviewLayout = findViewById(R.id.reply_preview_layout);
        replyPreviewName = findViewById(R.id.reply_preview_name);
        replyPreviewText = findViewById(R.id.reply_preview_text);
        btnCancelReply = findViewById(R.id.btn_cancel_reply);

        adapter = new MessagesAdapter(messageList, currentUserId);
        adapter.setGroup(true);
        
        // Reply Listeners
        adapter.setOnReplyClickListener(message -> {
            replyTargetMessage = message;
            showReplyPreview(message);
        });

        adapter.setOnMessageNavigateListener(messageId -> {
            int position = adapter.getMessagePosition(messageId);
            if (position != -1) {
                recyclerView.scrollToPosition(position);
                adapter.highlightMessage(messageId);
            } else {
                Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchCurrentUserInfo();
        loadMessages();

        sendBtn.setOnClickListener(v -> sendReply());
        btnAiReply.setOnClickListener(v -> generateAiReply());
        btnCancelReply.setOnClickListener(v -> cancelReply());
    }

    private void showReplyPreview(Message message) {
        replyPreviewName.setText(message.getSenderName());
        replyPreviewText.setText(message.getText());
        replyPreviewLayout.setVisibility(View.VISIBLE);
        replyEdit.requestFocus();
    }

    private void cancelReply() {
        replyTargetMessage = null;
        replyPreviewLayout.setVisibility(View.GONE);
    }

    private void fetchCurrentUserInfo() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUserName = doc.getString("username");
            }
        });
    }

    private void generateAiReply() {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append(parentAuthor).append(": ").append(parentContent).append("\n");
        
        int start = Math.max(0, messageList.size() - 9);
        for (int i = start; i < messageList.size(); i++) {
            Message m = messageList.get(i);
            contextBuilder.append(m.getSenderName()).append(": ").append(m.getText()).append("\n");
        }

        callAiService(contextBuilder.toString().trim());
    }

    private void callAiService(String context) {
        btnAiReply.setEnabled(false);
        aiProgressBar.setVisibility(View.VISIBLE);
        
        aiService.generateReply(currentUserName, context, new AiService.AiCallback() {
            @Override
            public void onGenerated(String text) {
                btnAiReply.setEnabled(true);
                aiProgressBar.setVisibility(View.GONE);
                replyEdit.setText(text);
                replyEdit.setSelection(text.length());
            }

            @Override
            public void onError(Exception e) {
                btnAiReply.setEnabled(true);
                aiProgressBar.setVisibility(View.GONE);
                Toast.makeText(ThreadActivity.this, "AI Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            Message message = doc.toObject(Message.class);
                            message.setMessageId(doc.getId());
                            messageList.add(message);
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty() && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendReply() {
        String text = replyEdit.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("senderName", currentUserName != null ? currentUserName : "Unknown");
        message.put("text", text);
        message.put("timestamp", new Date());

        if (replyTargetMessage != null) {
            message.put("replyToMessageId", replyTargetMessage.getMessageId());
            message.put("replyToText", replyTargetMessage.getText());
            message.put("replyToSenderName", replyTargetMessage.getSenderName());
        }

        db.collection("communities").document(communityId)
                .collection("posts").document(postId)
                .collection("replies").add(message)
                .addOnSuccessListener(aVoid -> {
                    replyEdit.setText("");
                    cancelReply();
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
