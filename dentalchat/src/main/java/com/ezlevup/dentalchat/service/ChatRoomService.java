package com.ezlevup.dentalchat.service;

import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.repository.ChatRoomRepository;
import com.ezlevup.dentalchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatRoom createChatRoom(User customer, String customerNotes) {
        String roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(roomId);
        chatRoom.setCustomer(customer);
        chatRoom.setStatus(ChatRoom.RoomStatus.WAITING);
        chatRoom.setCustomerNotes(customerNotes);
        
        return chatRoomRepository.save(chatRoom);
    }

    public ChatRoom assignAdmin(String roomId, User admin) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        
        if (chatRoom.getStatus() != ChatRoom.RoomStatus.WAITING) {
            throw new IllegalStateException("대기 중인 채팅방만 상담원을 배정할 수 있습니다.");
        }
        
        chatRoom.setAdmin(admin);
        chatRoom.setStatus(ChatRoom.RoomStatus.ACTIVE);
        chatRoom.setStartedAt(LocalDateTime.now());
        
        return chatRoomRepository.save(chatRoom);
    }

    public ChatRoom findAvailableAdminAndAssign(String roomId) {
        List<User> availableAdmins = userRepository.findAvailableAdmins();
        
        if (availableAdmins.isEmpty()) {
            throw new IllegalStateException("현재 사용 가능한 상담원이 없습니다.");
        }
        
        // 가장 적은 수의 활성 채팅방을 가진 상담원 선택
        User selectedAdmin = availableAdmins.stream()
                .min((admin1, admin2) -> {
                    int activeRooms1 = chatRoomRepository.findActiveRoomsByAdmin(admin1).size();
                    int activeRooms2 = chatRoomRepository.findActiveRoomsByAdmin(admin2).size();
                    return Integer.compare(activeRooms1, activeRooms2);
                })
                .orElseThrow(() -> new IllegalStateException("상담원을 선택할 수 없습니다."));
        
        return assignAdmin(roomId, selectedAdmin);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoom> findByRoomId(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findWaitingRooms() {
        return chatRoomRepository.findWaitingRoomsOrderByCreatedAt();
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findActiveRoomsByAdmin(User admin) {
        return chatRoomRepository.findActiveRoomsByAdmin(admin);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findActiveRoomsByUser(User user) {
        return chatRoomRepository.findActiveRoomsByUser(user);
    }

    public ChatRoom endChatRoom(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        
        if (chatRoom.getStatus() != ChatRoom.RoomStatus.ACTIVE) {
            throw new IllegalStateException("활성 상태인 채팅방만 종료할 수 있습니다.");
        }
        
        chatRoom.setStatus(ChatRoom.RoomStatus.ENDED);
        chatRoom.setEndedAt(LocalDateTime.now());
        
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional(readOnly = true)
    public boolean isUserInRoom(String roomId, String username) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findByRoomId(roomId);
        
        return chatRoom.map(room -> 
            (room.getCustomer() != null && room.getCustomer().getUsername().equals(username)) ||
            (room.getAdmin() != null && room.getAdmin().getUsername().equals(username))
        ).orElse(false);
    }
}