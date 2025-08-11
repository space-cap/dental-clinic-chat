package com.ezlevup.dentalchat.dto;

import java.time.LocalDateTime;

public record ChatMessage(
    String content,
    String sender,
    UserRole senderRole,
    MessageType type,
    LocalDateTime timestamp,
    String roomId
) {
    public ChatMessage withTimestamp() {
        return new ChatMessage(content, sender, senderRole, type, LocalDateTime.now(), roomId);
    }
    
    public static ChatMessage of(String content, String sender, UserRole senderRole, 
                                MessageType type, String roomId) {
        return new ChatMessage(content, sender, senderRole, type, LocalDateTime.now(), roomId);
    }
}