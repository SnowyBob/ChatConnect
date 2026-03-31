package com.example.chatconnect.adapters;

import android.content.Intent;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatconnect.R;
import com.example.chatconnect.activities.ThreadActivity;
import com.example.chatconnect.models.Post;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Post> posts;
    private MediaPlayer mediaPlayer;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.authorText.setText(post.getAuthorName());
        holder.contentText.setText(post.getContent());
        
        if (post.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.timeText.setText(sdf.format(post.getTimestamp().toDate()));
        }

        holder.aiBadge.setVisibility(post.isAiGenerated() ? View.VISIBLE : View.GONE);

        if (post.getAuthorProfileImageUrl() != null && !post.getAuthorProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getAuthorProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.authorImage);
        } else {
            holder.authorImage.setImageResource(R.drawable.ic_profile);
        }

        if (post.getVoiceUrl() != null && !post.getVoiceUrl().isEmpty()) {
            holder.playButton.setVisibility(View.VISIBLE);
            holder.playButton.setOnClickListener(v -> playAudio(post.getVoiceUrl()));
        } else {
            holder.playButton.setVisibility(View.GONE);
        }

        if (post.getReplyCount() > 0) {
            holder.replyCountText.setVisibility(View.VISIBLE);
            holder.replyCountText.setText(post.getReplyCount() + " replies");
        } else {
            holder.replyCountText.setVisibility(View.GONE);
        }

        holder.replyButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ThreadActivity.class);
            intent.putExtra("community_id", post.getCommunityId());
            intent.putExtra("post_id", post.getId());
            intent.putExtra("author_name", post.getAuthorName());
            intent.putExtra("post_content", post.getContent());
            v.getContext().startActivity(intent);
        });
    }

    private void playAudio(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView authorText, contentText, timeText, aiBadge, replyCountText;
        Button playButton, replyButton;
        ImageView authorImage;

        ViewHolder(View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.post_author);
            contentText = itemView.findViewById(R.id.post_content);
            timeText = itemView.findViewById(R.id.post_time);
            aiBadge = itemView.findViewById(R.id.ai_badge);
            playButton = itemView.findViewById(R.id.btn_play_voice);
            replyButton = itemView.findViewById(R.id.btn_reply);
            replyCountText = itemView.findViewById(R.id.reply_count);
            authorImage = itemView.findViewById(R.id.post_author_image);
        }
    }
}
