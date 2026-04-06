package com.example.chatconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatconnect.ChatActivity;
import com.example.chatconnect.R;
import com.example.chatconnect.adapters.SearchHistoryAdapter;
import com.example.chatconnect.adapters.SearchResultAdapter;
import com.example.chatconnect.models.SearchResult;
import com.example.chatconnect.services.AiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private Button btnSearch;
    private ProgressBar progressBar;
    private RecyclerView historyRecyclerView, resultsRecyclerView;
    
    private SearchHistoryAdapter historyAdapter;
    private SearchResultAdapter resultsAdapter;
    
    private List<String> searchHistory = new ArrayList<>();
    private List<SearchResult> searchResults = new ArrayList<>();
    
    private FirebaseFirestore db;
    private AiService aiService;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        aiService = new AiService();
        currentUserId = FirebaseAuth.getInstance().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        progressBar = findViewById(R.id.search_progress);
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        resultsRecyclerView = findViewById(R.id.results_recycler_view);

        loadSearchHistory();

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new SearchHistoryAdapter(searchHistory, query -> {
            searchInput.setText(query);
            performSearch(query);
        });
        historyRecyclerView.setAdapter(historyAdapter);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter = new SearchResultAdapter(searchResults, result -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chat_id", result.getChatId());
            intent.putExtra("chat_name", result.getChatName());
            intent.putExtra("scroll_to_message_id", result.getMessageId());
            startActivity(intent);
        });
        resultsRecyclerView.setAdapter(resultsAdapter);

        btnSearch.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
    }

    private void performSearch(String userPrompt) {
        saveSearchHistory(userPrompt);
        progressBar.setVisibility(View.VISIBLE);
        searchResults.clear();
        resultsAdapter.notifyDataSetChanged();

        // 1. Fetch messages from all user's chats (limit for performance)
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(chatSnapshots -> {
                    List<String> chatIds = new ArrayList<>();
                    final StringBuilder formattedMessages = new StringBuilder();
                    
                    for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                        chatIds.add(chatDoc.getId());
                    }

                    if (chatIds.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "No chats found to search.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // For simplicity, we fetch recent messages from each chat
                    // In a real app, you'd probably use a more centralized index or fetch selectively
                    fetchMessagesAndSearch(chatSnapshots, userPrompt);
                });
    }

    private void fetchMessagesAndSearch(com.google.firebase.firestore.QuerySnapshot chatSnapshots, String userPrompt) {
        final StringBuilder allMessagesBuilder = new StringBuilder();
        final int totalChats = chatSnapshots.size();
        final int[] processedChats = {0};

        for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
            String chatId = chatDoc.getId();
            String chatName = chatDoc.getString("name");
            if (chatName == null) chatName = "Direct Chat";

            final String finalChatName = chatName;
            db.collection("chats").document(chatId).collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50) // Limit to recent 50 messages per chat
                    .get()
                    .addOnCompleteListener(task -> {
                        processedChats[0]++;
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot msgDoc : task.getResult()) {
                                allMessagesBuilder.append("ChatId: ").append(chatId).append("\n");
                                allMessagesBuilder.append("Chat: ").append(finalChatName).append("\n");
                                allMessagesBuilder.append("User: ").append(msgDoc.getString("senderName")).append("\n");
                                allMessagesBuilder.append("Message: ").append(msgDoc.getString("text")).append("\n");
                                allMessagesBuilder.append("MessageId: ").append(msgDoc.getId()).append("\n");
                                allMessagesBuilder.append("Timestamp: ").append(msgDoc.get("timestamp")).append("\n\n");
                            }
                        }

                        if (processedChats[0] == totalChats) {
                            // All messages collected, call AI
                            aiService.searchMessages(allMessagesBuilder.toString(), userPrompt, new AiService.AiCallback() {
                                @Override
                                public void onGenerated(String jsonResult) {
                                    parseAndDisplayResults(jsonResult);
                                }

                                @Override
                                public void onError(Exception e) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(SearchActivity.this, "AI Search Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        }
    }

    private void parseAndDisplayResults(String json) {
        progressBar.setVisibility(View.GONE);
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SearchResult res = new SearchResult();
                res.setMessageId(obj.getString("messageId"));
                res.setChatId(obj.getString("chatId"));
                res.setChatName(obj.getString("chatName"));
                res.setSenderName(obj.getString("senderName"));
                res.setMessageText(obj.getString("messageText"));
                searchResults.add(res);
            }
            resultsAdapter.notifyDataSetChanged();
            if (searchResults.isEmpty()) {
                Toast.makeText(this, "No relevant messages found.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing search results: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSearchHistory() {
        android.content.SharedPreferences prefs = getSharedPreferences("SearchPrefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("history", new HashSet<>());
        searchHistory.clear();
        searchHistory.addAll(set);
    }

    private void saveSearchHistory(String query) {
        android.content.SharedPreferences prefs = getSharedPreferences("SearchPrefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("history", new HashSet<>());
        set.add(query);
        prefs.edit().putStringSet("history", set).apply();
        
        if (!searchHistory.contains(query)) {
            searchHistory.add(0, query);
            historyAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
