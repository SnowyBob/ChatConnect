package com.example.chatconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private Button btnSearch, btnLoadMore;
    private ProgressBar progressBar;
    private TextView searchStatus;
    private RecyclerView historyRecyclerView, resultsRecyclerView;

    private SearchHistoryAdapter historyAdapter;
    private SearchResultAdapter resultsAdapter;

    private List<String> searchHistory = new ArrayList<>();
    private List<SearchResult> searchResults = new ArrayList<>();

    // Store all fuzzy matches for "Load More"
    private List<MatchedMessage> allFuzzyMatches = new ArrayList<>();
    private int currentBatchStart = 0;
    private static final int BATCH_SIZE = 50;
    private String currentQuery = "";

    private FirebaseFirestore db;
    private AiService aiService;
    private String currentUserId;

    // Holds a matched message before AI ranking
    private static class MatchedMessage {
        String messageId, chatId, chatName, senderName, messageText;
        int matchCount; // how many search terms matched

        String toFormattedString() {
            return "ChatId: " + chatId + "\n" +
                    "Chat: " + chatName + "\n" +
                    "User: " + senderName + "\n" +
                    "Message: " + messageText + "\n" +
                    "MessageId: " + messageId + "\n\n";
        }
    }

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
        searchStatus = findViewById(R.id.search_status);
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        resultsRecyclerView = findViewById(R.id.results_recycler_view);
        btnLoadMore = findViewById(R.id.btn_load_more);

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

        btnLoadMore.setOnClickListener(v -> loadMoreResults());
    }

    private void performSearch(String userPrompt) {
        saveSearchHistory(userPrompt);
        currentQuery = userPrompt;
        currentBatchStart = 0;
        allFuzzyMatches.clear();
        searchResults.clear();
        resultsAdapter.notifyDataSetChanged();
        btnLoadMore.setVisibility(View.GONE);

        progressBar.setVisibility(View.VISIBLE);
        setStatus("Generating search terms...");

        // STEP 1: AI generates search terms
        aiService.generateSearchTerms(userPrompt, new AiService.AiCallback() {
            @Override
            public void onGenerated(String jsonTerms) {
                List<String> searchTerms = parseSearchTerms(jsonTerms);
                if (searchTerms.isEmpty()) {
                    // Fallback: just use the raw query words
                    String[] words = userPrompt.toLowerCase().split("\\s+");
                    for (String w : words) {
                        if (!w.isEmpty()) searchTerms.add(w);
                    }
                }
                setStatus("Searching all messages...");
                // STEP 2: Fetch ALL messages and do local fuzzy search
                fetchAllMessagesAndFuzzySearch(searchTerms);
            }

            @Override
            public void onError(Exception e) {
                // Fallback: use raw query words
                List<String> fallback = new ArrayList<>();
                for (String w : userPrompt.toLowerCase().split("\\s+")) {
                    if (!w.isEmpty()) fallback.add(w);
                }
                setStatus("Searching all messages...");
                fetchAllMessagesAndFuzzySearch(fallback);
            }
        });
    }

    private List<String> parseSearchTerms(String json) {
        List<String> terms = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String term = array.getString(i).toLowerCase().trim();
                if (!term.isEmpty()) terms.add(term);
            }
        } catch (Exception e) {
            // If parsing fails, return empty
        }
        return terms;
    }

    private void addMatch(com.google.firebase.firestore.DocumentSnapshot msgDoc, String chatId, String chatName, int matchCount) {
        String msgId = msgDoc.getId();
        for (MatchedMessage existing : allFuzzyMatches) {
            if (existing.messageId.equals(msgId)) {
                existing.matchCount = Math.max(existing.matchCount, matchCount);
                return;
            }
        }
        MatchedMessage match = new MatchedMessage();
        match.messageId = msgId;
        match.chatId = chatId;
        match.chatName = chatName;
        match.senderName = msgDoc.getString("senderName");
        match.messageText = msgDoc.getString("text");
        match.matchCount = matchCount;
        allFuzzyMatches.add(match);
    }

    private void fetchAllMessagesAndFuzzySearch(List<String> searchTerms) {
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(chatSnapshots -> {
                    int totalChats = chatSnapshots.size();
                    if (totalChats == 0) {
                        progressBar.setVisibility(View.GONE);
                        setStatus("No chats found.");
                        return;
                    }

                    final int[] processedChats = {0};

                    for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                        String chatId = chatDoc.getId();
                        String chatName = chatDoc.getString("name");
                        if (chatName == null) chatName = "Direct Chat";

                        final String finalChatName = chatName;

                        // NO LIMIT — fetch all messages
                        db.collection("chats").document(chatId).collection("messages")
                                .orderBy("timestamp", Query.Direction.ASCENDING)
                                .get()
                                .addOnCompleteListener(task -> {
                                    processedChats[0]++;

                                    if (task.isSuccessful()) {
                                        List<com.google.firebase.firestore.DocumentSnapshot> docs = new ArrayList<>(task.getResult().getDocuments());

                                        for (int i = 0; i < docs.size(); i++) {
                                            com.google.firebase.firestore.DocumentSnapshot msgDoc = docs.get(i);
                                            String text = msgDoc.getString("text");
                                            if (text == null || text.isEmpty()) continue;

                                            int matchCount = countFuzzyMatches(text, searchTerms);
                                            if (matchCount > 0) {
                                                addMatch(msgDoc, chatId, finalChatName, matchCount);

                                                for (int offset = -2; offset <= 2; offset++) {
                                                    if (offset == 0) continue;
                                                    int neighborIdx = i + offset;
                                                    if (neighborIdx >= 0 && neighborIdx < docs.size()) {
                                                        com.google.firebase.firestore.DocumentSnapshot neighbor = docs.get(neighborIdx);
                                                        String neighborText = neighbor.getString("text");
                                                        if (neighborText != null && !neighborText.isEmpty()) {
                                                            addMatch(neighbor, chatId, finalChatName, 0);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (processedChats[0] == totalChats) {
                                        onAllMessagesFetched();
                                    }
                                });
                    }
                });
    }

    private void onAllMessagesFetched() {
        if (allFuzzyMatches.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            setStatus("No matches found.");
            Toast.makeText(this, "No relevant messages found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sort by match count (most matches first)
        allFuzzyMatches.sort((a, b) -> Integer.compare(b.matchCount, a.matchCount));

        setStatus("AI is ranking " + allFuzzyMatches.size() + " matches...");

        // STEP 3: Send first batch to AI for ranking
        currentBatchStart = 0;
        rankCurrentBatch();
    }

    private void rankCurrentBatch() {
        int end = Math.min(currentBatchStart + BATCH_SIZE, allFuzzyMatches.size());
        List<MatchedMessage> batch = allFuzzyMatches.subList(currentBatchStart, end);

        StringBuilder formatted = new StringBuilder();
        for (MatchedMessage m : batch) {
            formatted.append(m.toFormattedString());
        }

        aiService.rankSearchResults(formatted.toString(), currentQuery, new AiService.AiCallback() {
            @Override
            public void onGenerated(String jsonResult) {
                progressBar.setVisibility(View.GONE);
                parseAndDisplayResults(jsonResult);

                // Show "Load More" if there are more matches
                currentBatchStart = end;
                if (currentBatchStart < allFuzzyMatches.size()) {
                    btnLoadMore.setVisibility(View.VISIBLE);
                    setStatus(searchResults.size() + " results shown • " +
                            (allFuzzyMatches.size() - currentBatchStart) + " more matches available");
                } else {
                    btnLoadMore.setVisibility(View.GONE);
                    setStatus(searchResults.size() + " results found");
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                setStatus("AI ranking failed, showing raw matches");
                // Fallback: show matches without AI ranking
                showRawMatches(batch);
            }
        });
    }

    private void loadMoreResults() {
        if (currentBatchStart >= allFuzzyMatches.size()) {
            Toast.makeText(this, "No more results", Toast.LENGTH_SHORT).show();
            btnLoadMore.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLoadMore.setVisibility(View.GONE);
        setStatus("Loading more results...");
        rankCurrentBatch();
    }

    private void showRawMatches(List<MatchedMessage> matches) {
        for (MatchedMessage m : matches) {
            SearchResult res = new SearchResult();
            res.setMessageId(m.messageId);
            res.setChatId(m.chatId);
            res.setChatName(m.chatName);
            res.setSenderName(m.senderName);
            res.setMessageText(m.messageText);
            searchResults.add(res);
        }
        resultsAdapter.notifyDataSetChanged();
    }

    // ==================== FUZZY MATCHING ====================

    private int countFuzzyMatches(String message, List<String> searchTerms) {
        String[] words = message.toLowerCase().split("\\s+");
        int matchCount = 0;

        for (String term : searchTerms) {
            // Check exact contains first (fast path)
            if (message.toLowerCase().contains(term)) {
                matchCount++;
                continue;
            }
            // Fuzzy match against individual words
            for (String word : words) {
                int tolerance = word.length() < 5 ? 1 : 2;
                if (levenshtein(word, term) <= tolerance) {
                    matchCount++;
                    break;
                }
            }
        }
        return matchCount;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    // ==================== UI HELPERS ====================

    private void setStatus(String text) {
        if (searchStatus != null) {
            searchStatus.setVisibility(View.VISIBLE);
            searchStatus.setText(text);
        }
    }

    private void parseAndDisplayResults(String json) {
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

                // Avoid duplicates
                boolean duplicate = false;
                for (SearchResult existing : searchResults) {
                    if (existing.getMessageId().equals(res.getMessageId())) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) searchResults.add(res);
            }
            resultsAdapter.notifyDataSetChanged();
            if (searchResults.isEmpty()) {
                Toast.makeText(this, "No relevant messages found.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing results: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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