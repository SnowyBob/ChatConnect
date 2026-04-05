package com.example.chatconnect;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private ArrayList<Chat> chatList;

    public ChatsAdapter(ArrayList<Chat> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.nameTextView.setText(chat.getName());
        holder.lastMessageTextView.setText(chat.getLastMessage());

        if (chat.getProfileImageUrl() != null && !chat.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chat.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_profile);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("chat_id", chat.getId());
            intent.putExtra("chat_name", chat.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView lastMessageTextView;
        ShapeableImageView profileImageView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.user_name);
            lastMessageTextView = itemView.findViewById(R.id.last_message);
            profileImageView = itemView.findViewById(R.id.profile_image);
        }
    }
}
