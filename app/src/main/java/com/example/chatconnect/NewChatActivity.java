package com.example.chatconnect;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class NewChatActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private UsersAdapter adapter;
    private ArrayList<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        usersRecyclerView = findViewById(R.id.users_recycler_view);

        // Dummy data for users
        userList.add(new User("Jessica Thompson"));
        userList.add(new User("Michael Lee"));
        userList.add(new User("Emily Davis"));

        adapter = new UsersAdapter(userList, user -> {
            Intent intent = new Intent(NewChatActivity.this, MainActivity.class);
            intent.putExtra("new_chat_user_name", user.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        usersRecyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
