package com.ezlevup.dentalchat.repository;

import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    Optional<ChatRoom> findByRoomId(String roomId);
    
    List<ChatRoom> findByStatus(ChatRoom.RoomStatus status);
    
    List<ChatRoom> findByCustomer(User customer);
    
    List<ChatRoom> findByAdmin(User admin);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.status = 'WAITING' ORDER BY cr.createdAt ASC")
    List<ChatRoom> findWaitingRoomsOrderByCreatedAt();
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.admin = :admin AND cr.status = 'ACTIVE'")
    List<ChatRoom> findActiveRoomsByAdmin(User admin);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.customer = :user OR cr.admin = :user) AND cr.status = 'ACTIVE'")
    List<ChatRoom> findActiveRoomsByUser(User user);
}