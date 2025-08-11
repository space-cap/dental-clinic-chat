package com.ezlevup.dentalchat;

import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.repository.ChatRoomRepository;
import com.ezlevup.dentalchat.repository.UserRepository;
import com.ezlevup.dentalchat.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class SimpleIntegrationTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUsername("customer1");
        customer.setNickname("Customer 1");
        customer.setUserType(User.UserType.CUSTOMER);
        customer.setStatus(User.UserStatus.ONLINE);
        customer = userRepository.save(customer);

        admin = new User();
        admin.setUsername("admin1");
        admin.setNickname("Admin 1");
        admin.setUserType(User.UserType.ADMIN);
        admin.setStatus(User.UserStatus.ONLINE);
        admin = userRepository.save(admin);
    }

    @Test
    void testCreateChatRoomIntegration() {
        String customerNotes = "치아 통증으로 상담 요청";
        
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, customerNotes);
        
        assertThat(chatRoom).isNotNull();
        assertThat(chatRoom.getRoomId()).isNotNull();
        assertThat(chatRoom.getRoomId()).startsWith("room_");
        assertThat(chatRoom.getCustomer()).isEqualTo(customer);
        assertThat(chatRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.WAITING);
        assertThat(chatRoom.getCustomerNotes()).isEqualTo(customerNotes);
        assertThat(chatRoom.getCreatedAt()).isNotNull();
        
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(1);
        
        ChatRoom savedRoom = chatRoomRepository.findByRoomId(chatRoom.getRoomId()).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getCustomerNotes()).isEqualTo(customerNotes);
    }

    @Test
    void testAssignAdminIntegration() {
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, "상담 요청");
        String roomId = chatRoom.getRoomId();
        
        ChatRoom assignedRoom = chatRoomService.assignAdmin(roomId, admin);
        
        assertThat(assignedRoom.getAdmin()).isEqualTo(admin);
        assertThat(assignedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ACTIVE);
        assertThat(assignedRoom.getStartedAt()).isNotNull();
        
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(0);
        
        ChatRoom savedRoom = chatRoomRepository.findByRoomId(roomId).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getAdmin()).isEqualTo(admin);
        assertThat(savedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ACTIVE);
    }

    @Test
    void testFindAvailableAdminAndAssignIntegration() {
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, "자동 매칭 테스트");
        String roomId = chatRoom.getRoomId();
        
        ChatRoom assignedRoom = chatRoomService.findAvailableAdminAndAssign(roomId);
        
        assertThat(assignedRoom.getAdmin()).isNotNull();
        assertThat(assignedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ACTIVE);
        assertThat(assignedRoom.getStartedAt()).isNotNull();
        
        List<User> availableAdmins = userRepository.findAvailableAdmins();
        assertThat(availableAdmins).contains(admin);
    }

    @Test
    void testEndChatRoomIntegration() {
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, "종료 테스트");
        ChatRoom activeRoom = chatRoomService.assignAdmin(chatRoom.getRoomId(), admin);
        
        ChatRoom endedRoom = chatRoomService.endChatRoom(activeRoom.getRoomId());
        
        assertThat(endedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ENDED);
        assertThat(endedRoom.getEndedAt()).isNotNull();
        
        ChatRoom savedRoom = chatRoomRepository.findByRoomId(activeRoom.getRoomId()).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ENDED);
    }

    @Test
    void testMultipleRoomsIntegration() {
        User customer2 = new User();
        customer2.setUsername("customer2");
        customer2.setNickname("Customer 2");
        customer2.setUserType(User.UserType.CUSTOMER);
        customer2.setStatus(User.UserStatus.ONLINE);
        customer2 = userRepository.save(customer2);

        ChatRoom room1 = chatRoomService.createChatRoom(customer, "첫 번째 방");
        ChatRoom room2 = chatRoomService.createChatRoom(customer2, "두 번째 방");
        
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(2);
        
        List<ChatRoom> waitingRooms = chatRoomService.findWaitingRooms();
        assertThat(waitingRooms).hasSize(2);
        assertThat(waitingRooms).extracting(ChatRoom::getStatus)
                .containsOnly(ChatRoom.RoomStatus.WAITING);
        
        chatRoomService.assignAdmin(room1.getRoomId(), admin);
        
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(1);
        
        List<ChatRoom> activeRooms = chatRoomService.findActiveRoomsByAdmin(admin);
        assertThat(activeRooms).hasSize(1);
        assertThat(activeRooms.get(0).getRoomId()).isEqualTo(room1.getRoomId());
    }

    @Test
    void testIsUserInRoomIntegration() {
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, "사용자 권한 테스트");
        chatRoomService.assignAdmin(chatRoom.getRoomId(), admin);
        
        boolean customerInRoom = chatRoomService.isUserInRoom(chatRoom.getRoomId(), customer.getUsername());
        boolean adminInRoom = chatRoomService.isUserInRoom(chatRoom.getRoomId(), admin.getUsername());
        boolean strangerInRoom = chatRoomService.isUserInRoom(chatRoom.getRoomId(), "stranger");
        
        assertThat(customerInRoom).isTrue();
        assertThat(adminInRoom).isTrue();
        assertThat(strangerInRoom).isFalse();
    }

    @Test
    void testExceptionHandlingIntegration() {
        assertThrows(IllegalArgumentException.class, () -> {
            chatRoomService.assignAdmin("nonexistent-room", admin);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            chatRoomService.endChatRoom("nonexistent-room");
        });
        
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, "예외 처리 테스트");
        
        assertThrows(IllegalStateException.class, () -> {
            chatRoomService.endChatRoom(chatRoom.getRoomId());
        });
    }

    @Test
    void testProcessNextWaitingCustomerIntegration() {
        chatRoomService.createChatRoom(customer, "대기열 테스트");
        
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(1);
        
        ChatRoom processedRoom = chatRoomService.processNextWaitingCustomer();
        
        assertThat(processedRoom).isNotNull();
        assertThat(processedRoom.getAdmin()).isNotNull();
        assertThat(processedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ACTIVE);
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(0);
    }

    @Test
    void testFullWorkflowIntegration() {
        ChatRoom chatRoom = chatRoomService.createChatRoom(customer, "전체 워크플로우 테스트");
        
        assertThat(chatRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.WAITING);
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(1);
        
        ChatRoom activeRoom = chatRoomService.findAvailableAdminAndAssign(chatRoom.getRoomId());
        
        assertThat(activeRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ACTIVE);
        assertThat(activeRoom.getAdmin()).isNotNull();
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(0);
        
        ChatRoom endedRoom = chatRoomService.endChatRoom(activeRoom.getRoomId());
        
        assertThat(endedRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ENDED);
        assertThat(endedRoom.getEndedAt()).isNotNull();
        
        ChatRoom finalRoom = chatRoomRepository.findByRoomId(chatRoom.getRoomId()).orElse(null);
        assertThat(finalRoom).isNotNull();
        assertThat(finalRoom.getStatus()).isEqualTo(ChatRoom.RoomStatus.ENDED);
    }
}