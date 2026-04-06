package com.example.chatconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.R;
import com.example.chatconnect.models.SearchResult;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<SearchResult> results;
    private OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    public SearchResultAdapter(List<SearchResult> results, OnResultClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.title.setText(result.getChatName() + " - " + result.getSenderName());
        holder.title.setTextColor(android.graphics.Color.WHITE);
        holder.text.setText(result.getMessageText());
        holder.text.setTextColor(android.graphics.Color.LTGRAY);
        holder.itemView.setOnClickListener(v -> listener.onResultClick(result));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, text;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            text = itemView.findViewById(android.R.id.text2);
        }
    }
}
