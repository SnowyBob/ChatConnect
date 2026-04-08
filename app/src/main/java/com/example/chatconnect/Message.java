package com.example.chatconnect;

import java.util.Date;

public class Message {
    private String messageId;
    private String text;
    private String senderId;
    private String senderName;
    private String senderProfileImageUrl;
    private Date timestamp;
    
    // Reply fields
    private String replyToMessageId;
    private String replyToText;
    private String replyToSenderName;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String text, String senderId, String senderName, Date timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = timestamp;
    }

    // Voice message fields
    private String audioUrl;
    private long audioDuration; // in milliseconds
    private boolean voiceMessage;

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public long getAudioDuration() { return audioDuration; }
    public void setAudioDuration(long audioDuration) { this.audioDuration = audioDuration; }

    public boolean isVoiceMessage() { return voiceMessage; }
    public void setVoiceMessage(boolean voiceMessage) { this.voiceMessage = voiceMessage; }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderProfileImageUrl() {
        return senderProfileImageUrl;
    }

    public void setSenderProfileImageUrl(String senderProfileImageUrl) {
        this.senderProfileImageUrl = senderProfileImageUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public String getReplyToSenderName() {
        return replyToSenderName;
    }

    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }
}
