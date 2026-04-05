package com.example.chatconnect;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUserId;
    private boolean isGroup = false;
    private OnReplyClickListener replyClickListener;
    private OnMessageNavigateListener navigateListener;
    private String highlightedMessageId = null;
    
    // Cache for user profile URLs to avoid repeated Firestore queries
    private final Map<String, String> profileCache = new HashMap<>();

    public interface OnReplyClickListener {
        void onReplyClick(Message message);
    }

    public interface OnMessageNavigateListener {
        void onMessageNavigate(String messageId);
    }

    public MessagesAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public void setOnMessageNavigateListener(OnMessageNavigateListener listener) {
        this.navigateListener = listener;
    }

    public void setGroup(boolean group) {
        isGroup = group;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message, isGroup && !message.getSenderId().equals(currentUserId));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public int getMessagePosition(String messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId() != null && messages.get(i).getMessageId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }

    public void highlightMessage(String messageId) {
        this.highlightedMessageId = messageId;
        int position = getMessagePosition(messageId);
        if (position != -1) {
            notifyItemChanged(position);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                highlightedMessageId = null;
                notifyItemChanged(position);
            }, 1000);
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, senderNameText;
        LinearLayout replyContainer;
        TextView replySenderName, replyContent;
        ImageView btnReply;
        View messageBubble;
        ShapeableImageView profileImage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            senderNameText = itemView.findViewById(R.id.sender_name_text);
            replyContainer = itemView.findViewById(R.id.reply_container);
            replySenderName = itemView.findViewById(R.id.reply_sender_name);
            replyContent = itemView.findViewById(R.id.reply_text);
            btnReply = itemView.findViewById(R.id.btn_reply);
            messageBubble = itemView.findViewById(R.id.message_bubble);
            profileImage = itemView.findViewById(R.id.message_profile_image);
        }

        void bind(Message message, boolean showSenderName) {
            messageText.setText(message.getText());
            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                timeText.setText(sdf.format(message.getTimestamp()));
            }

            if (senderNameText != null) {
                if (showSenderName && message.getSenderName() != null) {
                    senderNameText.setText(message.getSenderName());
                    senderNameText.setVisibility(View.VISIBLE);
                } else {
                    senderNameText.setVisibility(View.GONE);
                }
            }

            if (profileImage != null) {
                profileImage.setVisibility(View.VISIBLE);
                String senderId = message.getSenderId();
                String profileUrl = message.getSenderProfileImageUrl();

                // If message doesn't have URL, check cache or fetch from users collection
                if (profileUrl == null || profileUrl.isEmpty()) {
                    if (profileCache.containsKey(senderId)) {
                        loadProfileImage(profileCache.get(senderId));
                    } else if (senderId != null) {
                        FirebaseFirestore.getInstance().collection("users").document(senderId)
                                .get().addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        String url = doc.getString("profileImageUrl");
                                        if (url != null) {
                                            profileCache.put(senderId, url);
                                            loadProfileImage(url);
                                        }
                                    }
                                });
                        profileImage.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    loadProfileImage(profileUrl);
                }
            }

            // Handle reply display
            if (message.getReplyToMessageId() != null) {
                replyContainer.setVisibility(View.VISIBLE);
                replySenderName.setText(message.getReplyToSenderName());
                replyContent.setText(message.getReplyToText());
                replyContainer.setOnClickListener(v -> {
                    if (navigateListener != null) {
                        navigateListener.onMessageNavigate(message.getReplyToMessageId());
                    }
                });
            } else {
                replyContainer.setVisibility(View.GONE);
            }

            // Highlight effect
            if (highlightedMessageId != null && highlightedMessageId.equals(message.getMessageId())) {
                messageBubble.setBackgroundColor(Color.parseColor("#4481D4FA"));
            } else {
                messageBubble.setBackgroundResource(getItemViewType() == VIEW_TYPE_SENT ? 
                    R.drawable.sent_message_background : R.drawable.received_message_background);
            }

            btnReply.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    replyClickListener.onReplyClick(message);
                }
            });
        }

        private void loadProfileImage(String url) {
            if (profileImage == null) return;
            Log.d("PFP", "Loading URL: " + url);
            Glide.with(itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(profileImage);
        }
    }
}
