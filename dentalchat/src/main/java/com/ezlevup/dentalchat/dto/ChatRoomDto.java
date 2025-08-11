package com.ezlevup.dentalchat.dto;

import com.ezlevup.dentalchat.entity.ChatRoom.RoomStatus;

public class ChatRoomDto {
    private String roomId;
    private String customerNickname;
    private String adminNickname;
    private RoomStatus status;
    private String createdAt;
    private String customerNotes;

    public ChatRoomDto() {
    }

    public ChatRoomDto(String roomId, String customerNickname, String adminNickname, 
                      RoomStatus status, String customerNotes) {
        this.roomId = roomId;
        this.customerNickname = customerNickname;
        this.adminNickname = adminNickname;
        this.status = status;
        this.customerNotes = customerNotes;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCustomerNickname() {
        return customerNickname;
    }

    public void setCustomerNickname(String customerNickname) {
        this.customerNickname = customerNickname;
    }

    public String getAdminNickname() {
        return adminNickname;
    }

    public void setAdminNickname(String adminNickname) {
        this.adminNickname = adminNickname;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }
}