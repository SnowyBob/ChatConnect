package com.example.chatconnect;

public class Chat {
    private String id;
    private String name;
    private String lastMessage;
    private boolean isGroup;
    private String profileImageUrl;
    private int unreadCount;
    private long timestamp;

    public Chat() {}

    public Chat(String id, String name, String lastMessage, boolean isGroup, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.isGroup = isGroup;
        this.profileImageUrl = profileImageUrl;
        this.unreadCount = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public boolean isGroup() { return isGroup; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
