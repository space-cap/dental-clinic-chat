package com.ezlevup.dentalchat.controller;

import com.ezlevup.dentalchat.dto.ChatMessage;
import com.ezlevup.dentalchat.dto.MessageType;
import com.ezlevup.dentalchat.dto.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @MessageMapping("/chat.sendMessage/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId, ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        return processMessageAsync(roomId, chatMessage, MessageType.CHAT, headerAccessor)
                .join();
    }

    @MessageMapping("/chat.joinRoom/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage joinRoom(@DestinationVariable String roomId, ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        headerAccessor.getSessionAttributes().put("username", chatMessage.sender());
        
        logger.info("User {} joined room {} (session: {})", chatMessage.sender(), roomId, sessionId);
        
        ChatMessage joinMessage = ChatMessage.of(
            chatMessage.sender() + " joined the room",
            chatMessage.sender(),
            chatMessage.senderRole(),
            MessageType.JOIN,
            roomId
        );
        
        return processMessageAsync(roomId, joinMessage, MessageType.JOIN, headerAccessor)
                .join();
    }

    private CompletableFuture<ChatMessage> processMessageAsync(String roomId, ChatMessage message, MessageType messageType, SimpMessageHeaderAccessor headerAccessor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateMessage(message, roomId);
                
                ChatMessage processedMessage = new ChatMessage(
                    message.content(),
                    message.sender(),
                    message.senderRole(),
                    messageType,
                    message.timestamp(),
                    roomId
                ).withTimestamp();
                
                logMessage(processedMessage, headerAccessor.getSessionId());
                
                return processedMessage;
                
            } catch (Exception e) {
                logger.error("Error processing message in room {}: {}", roomId, e.getMessage(), e);
                throw new RuntimeException("Failed to process message", e);
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    private void validateMessage(ChatMessage message, String roomId) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (message.content() == null || message.content().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (message.sender() == null || message.sender().trim().isEmpty()) {
            throw new IllegalArgumentException("Sender cannot be empty");
        }
        
        if (message.senderRole() == null) {
            throw new IllegalArgumentException("Sender role cannot be null");
        }
        
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be empty");
        }
        
        if (message.content().length() > 1000) {
            throw new IllegalArgumentException("Message content too long (max 1000 characters)");
        }
        
        logger.debug("Message validation successful for room: {}, sender: {}", roomId, message.sender());
    }

    private void logMessage(ChatMessage message, String sessionId) {
        logger.info("Message processed - Room: {}, Sender: {}, Role: {}, Type: {}, Session: {}, Content: {}", 
            message.roomId(),
            message.sender(),
            message.senderRole(),
            message.type(),
            sessionId,
            truncateContent(message.content())
        );
    }

    private String truncateContent(String content) {
        if (content == null) return "null";
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }
}