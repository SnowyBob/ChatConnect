package com.example.chatconnect.models;

import com.google.firebase.Timestamp;

/**
 * Model class for a Community Post.
 */
public class Post {
    private String id;
    private String communityId;
    private String authorId;
    private String authorName;
    private String authorProfileImageUrl;
    private String content;
    private Timestamp timestamp;
    private String voiceUrl;
    private boolean aiGenerated;
    private int replyCount;

    public Post() {}

    public Post(String communityId, String authorId, String authorName, String content) {
        this.communityId = communityId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.timestamp = Timestamp.now();
        this.aiGenerated = false;
        this.replyCount = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCommunityId() { return communityId; }
    public void setCommunityId(String communityId) { this.communityId = communityId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorProfileImageUrl() { return authorProfileImageUrl; }
    public void setAuthorProfileImageUrl(String authorProfileImageUrl) { this.authorProfileImageUrl = authorProfileImageUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String voiceUrl) { this.voiceUrl = voiceUrl; }

    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
}
