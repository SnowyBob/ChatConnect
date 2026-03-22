package com.example.chatconnect.adapters;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatconnect.R;
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

        if (post.getVoiceUrl() != null && !post.getVoiceUrl().isEmpty()) {
            holder.playButton.setVisibility(View.VISIBLE);
            holder.playButton.setOnClickListener(v -> playAudio(post.getVoiceUrl()));
        } else {
            holder.playButton.setVisibility(View.GONE);
        }
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
        TextView authorText, contentText, timeText, aiBadge;
        Button playButton;

        ViewHolder(View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.post_author);
            contentText = itemView.findViewById(R.id.post_content);
            timeText = itemView.findViewById(R.id.post_time);
            aiBadge = itemView.findViewById(R.id.ai_badge);
            playButton = itemView.findViewById(R.id.btn_play_voice);
        }
    }
}
