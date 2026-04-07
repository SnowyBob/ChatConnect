package com.example.chatconnect.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for a Community.
 */
public class Community {
    private String id;
    private String name;
    private String description;
    private String topic;
    private String ownerId;
    // Map of userId to role string
    private Map<String, String> members;
    // List of banned user IDs
    private List<String> bannedUsers;
    // Map of userId to unread count
    private Map<String, Long> unreadCounts;

    public Community() {
        this.members = new HashMap<>();
        this.bannedUsers = new ArrayList<>();
        this.unreadCounts = new HashMap<>();
    }

    public Community(String id, String name, String description, String topic, String ownerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.topic = topic;
        this.ownerId = ownerId;
        this.members = new HashMap<>();
        this.members.put(ownerId, Role.OWNER.name());
        this.bannedUsers = new ArrayList<>();
        this.unreadCounts = new HashMap<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public Map<String, String> getMembers() { return members; }
    public void setMembers(Map<String, String> members) { this.members = members; }

    public List<String> getBannedUsers() { return bannedUsers; }
    public void setBannedUsers(List<String> bannedUsers) { this.bannedUsers = bannedUsers; }

    public Map<String, Long> getUnreadCounts() { return unreadCounts; }
    public void setUnreadCounts(Map<String, Long> unreadCounts) { this.unreadCounts = unreadCounts; }

    public int getUnreadCount(String userId) {
        if (unreadCounts != null && unreadCounts.containsKey(userId)) {
            return unreadCounts.get(userId).intValue();
        }
        return 0;
    }

    /**
     * Helper to get Role for a specific user.
     */
    public Role getUserRole(String userId) {
        if (members == null || !members.containsKey(userId)) {
            return null; // Not a member
        }
        return Role.fromString(members.get(userId));
    }

    public boolean isBanned(String userId) {
        return bannedUsers != null && bannedUsers.contains(userId);
    }
}
