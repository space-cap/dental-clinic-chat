package com.ezlevup.dentalchat.service;

import com.ezlevup.dentalchat.dto.ChatMessage;
import com.ezlevup.dentalchat.dto.MessageType;
import com.ezlevup.dentalchat.dto.UserRole;
import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.Message;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.repository.ChatRoomRepository;
import com.ezlevup.dentalchat.repository.MessageRepository;
import com.ezlevup.dentalchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Message saveMessage(ChatMessage messageDto) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(messageDto.roomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        User sender = userRepository.findByUsername(messageDto.sender())
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));

        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setSender(sender);
        message.setContent(messageDto.content());
        
        // MessageType 변환 - record의 MessageType을 entity의 MessageType으로
        Message.MessageType entityMessageType;
        switch (messageDto.type()) {
            case CHAT -> entityMessageType = Message.MessageType.CHAT;
            case JOIN -> entityMessageType = Message.MessageType.JOIN;
            case LEAVE -> entityMessageType = Message.MessageType.LEAVE;
            default -> entityMessageType = Message.MessageType.CHAT;
        }
        message.setMessageType(entityMessageType);

        Message savedMessage = messageRepository.save(message);

        // WebSocket으로 메시지 전송
        UserRole responseRole = sender.getUserType() == User.UserType.ADMIN ? UserRole.ADMIN : UserRole.CUSTOMER;
        ChatMessage responseDto = new ChatMessage(
                messageDto.content(),
                sender.getUsername(),
                responseRole,
                messageDto.type(),
                savedMessage.getSentAt(),
                messageDto.roomId()
        );

        messagingTemplate.convertAndSend("/topic/room/" + messageDto.roomId(), responseDto);

        return savedMessage;
    }

    public Message saveSystemMessage(String roomId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setContent(content);
        message.setMessageType(Message.MessageType.SYSTEM);

        Message savedMessage = messageRepository.save(message);

        // WebSocket으로 시스템 메시지 전송
        ChatMessage responseDto = new ChatMessage(
                content,
                "system",
                UserRole.ADMIN,
                MessageType.CHAT,
                savedMessage.getSentAt(),
                roomId
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId, responseDto);

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<Message> findMessagesByRoomId(String roomId) {
        return messageRepository.findByRoomIdOrderBySentAtAsc(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(String roomId) {
        List<Message> messages = messageRepository.findByRoomIdOrderBySentAtAsc(roomId);
        
        return messages.stream()
                .map(message -> {
                    String sender = message.getSender() != null ? message.getSender().getUsername() : "system";
                    UserRole senderRole;
                    if (message.getSender() == null) {
                        senderRole = UserRole.ADMIN; // 시스템 메시지
                    } else {
                        senderRole = message.getSender().getUserType() == User.UserType.ADMIN ? UserRole.ADMIN : UserRole.CUSTOMER;
                    }
                    
                    // Entity MessageType을 DTO MessageType으로 변환
                    MessageType dtoMessageType;
                    switch (message.getMessageType()) {
                        case CHAT, SYSTEM -> dtoMessageType = MessageType.CHAT;
                        case JOIN -> dtoMessageType = MessageType.JOIN;
                        case LEAVE -> dtoMessageType = MessageType.LEAVE;
                        default -> dtoMessageType = MessageType.CHAT;
                    }
                    
                    return new ChatMessage(
                            message.getContent(),
                            sender,
                            senderRole,
                            dtoMessageType,
                            message.getSentAt(),
                            roomId
                    );
                })
                .toList();
    }

    public void markMessagesAsRead(String roomId, String username) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Message> messages = messageRepository.findByChatRoomOrderBySentAtAsc(chatRoom);
        messages.stream()
                .filter(message -> !message.isRead() && !message.getSender().equals(currentUser))
                .forEach(message -> {
                    message.setRead(true);
                    messageRepository.save(message);
                });
    }
}