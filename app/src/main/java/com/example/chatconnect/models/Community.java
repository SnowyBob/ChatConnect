package com.example.chatconnect.models;

import java.util.HashMap;
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

    public Community() {
        this.members = new HashMap<>();
    }

    public Community(String id, String name, String description, String topic, String ownerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.topic = topic;
        this.ownerId = ownerId;
        this.members = new HashMap<>();
        this.members.put(ownerId, Role.OWNER.name());
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

    /**
     * Helper to get Role for a specific user.
     */
    public Role getUserRole(String userId) {
        if (members == null || !members.containsKey(userId)) {
            return null; // Not a member
        }
        return Role.fromString(members.get(userId));
    }
}
