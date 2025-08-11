package com.ezlevup.dentalchat.repository;

import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByChatRoomOrderBySentAtAsc(ChatRoom chatRoom);
    
    @Query("SELECT m FROM Message m WHERE m.chatRoom.roomId = :roomId ORDER BY m.sentAt ASC")
    List<Message> findByRoomIdOrderBySentAtAsc(String roomId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom = :chatRoom AND m.isRead = false AND m.sender != :currentUser")
    Long countUnreadMessagesByChatRoomAndNotSender(ChatRoom chatRoom, com.ezlevup.dentalchat.entity.User currentUser);
}