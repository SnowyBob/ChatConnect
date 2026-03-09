package com.example.chatconnect;

public class Chat {
    private String id;
    private String name;
    private String lastMessage;
    private boolean isGroup;

    public Chat() {}

    public Chat(String id, String name, String lastMessage, boolean isGroup) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.isGroup = isGroup;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public boolean isGroup() { return isGroup; }
}
