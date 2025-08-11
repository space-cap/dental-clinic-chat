package com.ezlevup.dentalchat.service;

import com.ezlevup.dentalchat.dto.ChatMessageDto;
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

    public Message saveMessage(ChatMessageDto messageDto) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(messageDto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        User sender = userRepository.findByUsername(messageDto.getSenderUsername())
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));

        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setSender(sender);
        message.setContent(messageDto.getContent());
        message.setMessageType(messageDto.getMessageType());

        Message savedMessage = messageRepository.save(message);

        // WebSocket으로 메시지 전송
        ChatMessageDto responseDto = new ChatMessageDto(
                messageDto.getRoomId(),
                sender.getUsername(),
                sender.getNickname(),
                messageDto.getContent(),
                messageDto.getMessageType()
        );
        responseDto.setTimestamp(savedMessage.getSentAt().format(formatter));

        messagingTemplate.convertAndSend("/topic/room/" + messageDto.getRoomId(), responseDto);

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
        ChatMessageDto responseDto = new ChatMessageDto(
                roomId,
                "system",
                "시스템",
                content,
                Message.MessageType.SYSTEM
        );
        responseDto.setTimestamp(savedMessage.getSentAt().format(formatter));

        messagingTemplate.convertAndSend("/topic/room/" + roomId, responseDto);

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<Message> findMessagesByRoomId(String roomId) {
        return messageRepository.findByRoomIdOrderBySentAtAsc(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(String roomId) {
        List<Message> messages = messageRepository.findByRoomIdOrderBySentAtAsc(roomId);
        
        return messages.stream()
                .map(message -> {
                    ChatMessageDto dto = new ChatMessageDto(
                            roomId,
                            message.getSender() != null ? message.getSender().getUsername() : "system",
                            message.getSender() != null ? message.getSender().getNickname() : "시스템",
                            message.getContent(),
                            message.getMessageType()
                    );
                    dto.setTimestamp(message.getSentAt().format(formatter));
                    return dto;
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