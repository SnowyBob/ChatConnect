package com.example.chatconnect.utils;

public class ChatState {
    private static String activeChatId = null;

    public static void setActiveChatId(String chatId) {
        activeChatId = chatId;
    }

    public static String getActiveChatId() {
        return activeChatId;
    }
}
