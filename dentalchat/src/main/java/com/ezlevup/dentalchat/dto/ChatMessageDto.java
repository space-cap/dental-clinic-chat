package com.ezlevup.dentalchat.dto;

import com.ezlevup.dentalchat.entity.Message.MessageType;

public class ChatMessageDto {
    private String roomId;
    private String senderUsername;
    private String senderNickname;
    private String content;
    private MessageType messageType;
    private String timestamp;

    public ChatMessageDto() {
    }

    public ChatMessageDto(String roomId, String senderUsername, String senderNickname, 
                         String content, MessageType messageType) {
        this.roomId = roomId;
        this.senderUsername = senderUsername;
        this.senderNickname = senderNickname;
        this.content = content;
        this.messageType = messageType;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}