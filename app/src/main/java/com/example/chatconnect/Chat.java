package com.example.chatconnect;

public class Chat {
    private String username;
    private String lastMessage;
    private String odId; // other user's ID

    public Chat(String username, String lastMessage, String odId) {
        this.username = username;
        this.lastMessage = lastMessage;
        this.odId = odId;
    }

    public String getUsername() { return username; }
    public String getLastMessage() { return lastMessage; }
    public String getOdId() { return odId; }
}