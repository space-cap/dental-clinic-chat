package com.ezlevup.dentalchat.service;

import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.repository.ChatRoomRepository;
import com.ezlevup.dentalchat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional
public class ChatRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private final ConcurrentLinkedQueue<String> waitingCustomers = new ConcurrentLinkedQueue<>();
    
    private final ConcurrentHashMap<String, LocalDateTime> sessionStartTimes = new ConcurrentHashMap<>();
    
    private final ReentrantLock queueLock = new ReentrantLock();

    public ChatRoom createChatRoom(User customer, String customerNotes) {
        String roomId = generateUniqueRoomId();
        
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(roomId);
        chatRoom.setCustomer(customer);
        chatRoom.setStatus(ChatRoom.RoomStatus.WAITING);
        chatRoom.setCustomerNotes(customerNotes);
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        addToWaitingQueue(roomId);
        logger.info("새 채팅방 생성: roomId={}, customer={}", roomId, customer.getUsername());
        
        return savedRoom;
    }

    private String generateUniqueRoomId() {
        String roomId;
        do {
            roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        } while (chatRoomRepository.findByRoomId(roomId).isPresent());
        
        return roomId;
    }

    public void addToWaitingQueue(String roomId) {
        queueLock.lock();
        try {
            if (!waitingCustomers.contains(roomId)) {
                waitingCustomers.offer(roomId);
                logger.info("고객 대기열에 추가: roomId={}, 대기열 크기={}", roomId, waitingCustomers.size());
            }
        } finally {
            queueLock.unlock();
        }
    }

    public String getNextWaitingCustomer() {
        queueLock.lock();
        try {
            String roomId = waitingCustomers.poll();
            if (roomId != null) {
                logger.info("대기열에서 다음 고객 선택: roomId={}, 남은 대기열 크기={}", roomId, waitingCustomers.size());
            }
            return roomId;
        } finally {
            queueLock.unlock();
        }
    }

    public int getWaitingQueueSize() {
        return waitingCustomers.size();
    }

    public void removeFromWaitingQueue(String roomId) {
        queueLock.lock();
        try {
            boolean removed = waitingCustomers.remove(roomId);
            if (removed) {
                logger.info("대기열에서 제거: roomId={}", roomId);
            }
        } finally {
            queueLock.unlock();
        }
    }

    public ChatRoom assignAdmin(String roomId, User admin) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        
        if (chatRoom.getStatus() != ChatRoom.RoomStatus.WAITING) {
            throw new IllegalStateException("대기 중인 채팅방만 상담원을 배정할 수 있습니다.");
        }
        
        chatRoom.setAdmin(admin);
        chatRoom.setStatus(ChatRoom.RoomStatus.ACTIVE);
        LocalDateTime startTime = LocalDateTime.now();
        chatRoom.setStartedAt(startTime);
        
        removeFromWaitingQueue(roomId);
        startSessionTimer(roomId, startTime);
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        logger.info("상담원 배정 완료: roomId={}, admin={}", roomId, admin.getUsername());
        
        return savedRoom;
    }

    private void startSessionTimer(String roomId, LocalDateTime startTime) {
        sessionStartTimes.put(roomId, startTime);
        logger.info("상담 세션 타이머 시작: roomId={}, startTime={}", roomId, startTime);
    }

    public ChatRoom findAvailableAdminAndAssign(String roomId) {
        List<User> availableAdmins = userRepository.findAvailableAdmins();
        
        if (availableAdmins.isEmpty()) {
            logger.warn("사용 가능한 상담원이 없습니다. roomId={}", roomId);
            throw new IllegalStateException("현재 사용 가능한 상담원이 없습니다.");
        }
        
        User selectedAdmin = availableAdmins.stream()
                .min((admin1, admin2) -> {
                    int activeRooms1 = chatRoomRepository.findActiveRoomsByAdmin(admin1).size();
                    int activeRooms2 = chatRoomRepository.findActiveRoomsByAdmin(admin2).size();
                    return Integer.compare(activeRooms1, activeRooms2);
                })
                .orElseThrow(() -> new IllegalStateException("상담원을 선택할 수 없습니다."));
        
        logger.info("자동 상담원 매칭: roomId={}, selectedAdmin={}", roomId, selectedAdmin.getUsername());
        return assignAdmin(roomId, selectedAdmin);
    }

    public ChatRoom processNextWaitingCustomer() {
        String nextRoomId = getNextWaitingCustomer();
        if (nextRoomId == null) {
            logger.info("대기 중인 고객이 없습니다.");
            return null;
        }
        
        try {
            return findAvailableAdminAndAssign(nextRoomId);
        } catch (IllegalStateException e) {
            addToWaitingQueue(nextRoomId);
            logger.warn("상담원 배정 실패로 다시 대기열에 추가: roomId={}", nextRoomId);
            throw e;
        }
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
        
        endSessionTimer(roomId);
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        logger.info("채팅방 종료: roomId={}", roomId);
        
        return savedRoom;
    }

    private void endSessionTimer(String roomId) {
        LocalDateTime startTime = sessionStartTimes.remove(roomId);
        if (startTime != null) {
            logger.info("상담 세션 타이머 종료: roomId={}, 상담시간={}분", 
                    roomId, java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes());
        }
    }

    public boolean isSessionExpired(String roomId) {
        LocalDateTime startTime = sessionStartTimes.get(roomId);
        if (startTime == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = java.time.Duration.between(startTime, now).toMinutes();
        
        return minutesElapsed >= SESSION_TIMEOUT_MINUTES;
    }

    @Scheduled(fixedRate = 60000)
    public void checkExpiredSessions() {
        logger.debug("만료된 상담 세션 확인 중...");
        
        List<String> expiredSessions = sessionStartTimes.entrySet().stream()
                .filter(entry -> {
                    long minutesElapsed = java.time.Duration.between(entry.getValue(), LocalDateTime.now()).toMinutes();
                    return minutesElapsed >= SESSION_TIMEOUT_MINUTES;
                })
                .map(entry -> entry.getKey())
                .toList();
        
        for (String roomId : expiredSessions) {
            try {
                Optional<ChatRoom> chatRoom = findByRoomId(roomId);
                if (chatRoom.isPresent() && chatRoom.get().getStatus() == ChatRoom.RoomStatus.ACTIVE) {
                    endChatRoom(roomId);
                    logger.warn("만료된 상담 세션 자동 종료: roomId={}", roomId);
                }
            } catch (Exception e) {
                logger.error("만료된 세션 종료 중 오류 발생: roomId={}", roomId, e);
            }
        }
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