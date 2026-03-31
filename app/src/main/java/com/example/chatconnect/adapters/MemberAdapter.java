package com.example.chatconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatconnect.R;
import com.example.chatconnect.User;
import com.example.chatconnect.models.Role;

import java.util.List;
import java.util.Map;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private List<User> members;
    private Map<String, String> roles;
    private String currentUserId;
    private OnMemberActionListener listener;

    public interface OnMemberActionListener {
        void onActionClick(User user);
    }

    public MemberAdapter(List<User> members, Map<String, String> roles, String currentUserId, OnMemberActionListener listener) {
        this.members = members;
        this.roles = roles;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateRoles(Map<String, String> roles) {
        this.roles = roles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = members.get(position);
        holder.nameText.setText(user.getName());
        
        String roleStr = roles.get(user.getUid());
        holder.roleText.setText(roleStr != null ? roleStr : "MEMBER");

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_profile);
        }

        // Only the Owner can see the action arrow to manage others.
        String myRoleStr = roles.get(currentUserId);
        Role myRole = Role.fromString(myRoleStr);
        
        boolean isMe = user.getUid().equals(currentUserId);
        
        if (myRole == Role.OWNER && !isMe) {
             holder.actionButton.setVisibility(View.VISIBLE);
        } else {
             holder.actionButton.setVisibility(View.GONE);
        }

        holder.actionButton.setOnClickListener(v -> listener.onActionClick(user));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, actionButton;
        TextView nameText, roleText;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.member_image);
            nameText = itemView.findViewById(R.id.member_name);
            roleText = itemView.findViewById(R.id.member_role);
            actionButton = itemView.findViewById(R.id.btn_member_action);
        }
    }
}
