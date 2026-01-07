package com.example.chatconnect;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter adapter;
    private ArrayList<Message> messageList = new ArrayList<>();
    private EditText messageInput;
    private ImageView sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        // Dummy messages
        messageList.add(new Message("Hey, how are you?", false));
        messageList.add(new Message("I'm good, thanks! What about you?", true));
        messageList.add(new Message("Doing well, just working on some projects.", false));

        adapter = new MessagesAdapter(messageList);
        messagesRecyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                messageList.add(new Message(messageText, true));
                adapter.notifyItemInserted(messageList.size() - 1);
                messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                messageInput.setText("");
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
