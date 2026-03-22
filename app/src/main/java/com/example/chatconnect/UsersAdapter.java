package com.example.chatconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> users;
    private final OnUserClickListener onUserClickListener;
    private boolean selectionMode = false;
    private Set<User> selectedUsers = new HashSet<>();

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onSelectionChanged(int count);
    }

    public UsersAdapter(List<User> users, OnUserClickListener onUserClickListener) {
        this.users = users;
        this.onUserClickListener = onUserClickListener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        if (!selectionMode) {
            selectedUsers.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public List<User> getSelectedUsers() {
        return new ArrayList<>(selectedUsers);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, onUserClickListener, selectionMode, selectedUsers.contains(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        CheckBox checkBox;
        ImageView profileImageView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.user_name);
            checkBox = itemView.findViewById(R.id.user_checkbox);
            profileImageView = itemView.findViewById(R.id.user_profile_image);
        }

        public void bind(final User user, final OnUserClickListener onUserClickListener, boolean isSelectionMode, boolean isSelected) {
            nameTextView.setText(user.getUsername());
            checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(isSelected);

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile);
            }

            itemView.setOnClickListener(v -> {
                if (isSelectionMode) {
                    if (selectedUsers.contains(user)) {
                        selectedUsers.remove(user);
                    } else {
                        selectedUsers.add(user);
                    }
                    notifyItemChanged(getAdapterPosition());
                    onUserClickListener.onSelectionChanged(selectedUsers.size());
                } else {
                    onUserClickListener.onUserClick(user);
                }
            });
        }
    }
}
