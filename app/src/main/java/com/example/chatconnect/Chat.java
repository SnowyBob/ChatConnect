package com.example.chatconnect;

public class Chat {
    private String userName;
    private String lastMessage;

    public Chat(String userName, String lastMessage) {
        this.userName = userName;
        this.lastMessage = lastMessage;
    }

    public String getUserName() {
        return userName;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
