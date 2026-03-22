package com.example.chatconnect.models;

/**
 * Roles for Community members.
 */
public enum Role {
    OWNER,
    ADMIN,
    MEMBER;

    public static Role fromString(String roleStr) {
        if (roleStr == null) return MEMBER;
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
