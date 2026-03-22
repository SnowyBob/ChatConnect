package com.example.chatconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatconnect.R;
import com.example.chatconnect.models.Community;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private List<Community> communities;
    private OnCommunityClickListener listener;

    public interface OnCommunityClickListener {
        void onCommunityClick(Community community);
    }

    public CommunityAdapter(List<Community> communities, OnCommunityClickListener listener) {
        this.communities = communities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Community community = communities.get(position);
        holder.nameText.setText(community.getName());
        holder.topicText.setText(community.getTopic());
        holder.descriptionText.setText(community.getDescription());
        holder.itemView.setOnClickListener(v -> listener.onCommunityClick(community));
    }

    @Override
    public int getItemCount() {
        return communities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, topicText, descriptionText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.community_name);
            topicText = itemView.findViewById(R.id.community_topic);
            descriptionText = itemView.findViewById(R.id.community_description);
        }
    }
}
