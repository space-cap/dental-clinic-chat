package com.ezlevup.dentalchat.service;

import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.repository.ChatRoomRepository;
import com.ezlevup.dentalchat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceUnitTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User customer;
    private User admin1;
    private User admin2;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("customer1");
        customer.setNickname("Customer 1");
        customer.setUserType(User.UserType.CUSTOMER);
        customer.setStatus(User.UserStatus.ONLINE);

        admin1 = new User();
        admin1.setId(2L);
        admin1.setUsername("admin1");
        admin1.setNickname("Admin 1");
        admin1.setUserType(User.UserType.ADMIN);
        admin1.setStatus(User.UserStatus.ONLINE);

        admin2 = new User();
        admin2.setId(3L);
        admin2.setUsername("admin2");
        admin2.setNickname("Admin 2");
        admin2.setUserType(User.UserType.ADMIN);
        admin2.setStatus(User.UserStatus.ONLINE);

        chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setRoomId("room_12345678");
        chatRoom.setCustomer(customer);
        chatRoom.setStatus(ChatRoom.RoomStatus.WAITING);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setCustomerNotes("치아 통증 상담");
    }

    @Test
    void testCreateChatRoom() {
        when(chatRoomRepository.findByRoomId(anyString())).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        String customerNotes = "치아 통증으로 상담 요청";
        ChatRoom result = chatRoomService.createChatRoom(customer, customerNotes);

        assertThat(result).isNotNull();
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getStatus()).isEqualTo(ChatRoom.RoomStatus.WAITING);
        
        verify(chatRoomRepository).save(any(ChatRoom.class));
        assertThat(chatRoomService.getWaitingQueueSize()).isGreaterThan(0);
    }

    @Test
    void testAssignAdmin() {
        when(chatRoomRepository.findByRoomId("room_12345678")).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        ChatRoom result = chatRoomService.assignAdmin("room_12345678", admin1);

        assertThat(result).isNotNull();
        verify(chatRoomRepository).findByRoomId("room_12345678");
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void testAssignAdminToNonExistentRoom() {
        when(chatRoomRepository.findByRoomId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            chatRoomService.assignAdmin("nonexistent", admin1)
        );
    }

    @Test
    void testFindAvailableAdminAndAssign() {
        List<User> availableAdmins = Arrays.asList(admin1, admin2);
        when(userRepository.findAvailableAdmins()).thenReturn(availableAdmins);
        when(chatRoomRepository.findActiveRoomsByAdmin(any(User.class))).thenReturn(Arrays.asList());
        when(chatRoomRepository.findByRoomId("room_12345678")).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        ChatRoom result = chatRoomService.findAvailableAdminAndAssign("room_12345678");

        assertThat(result).isNotNull();
        verify(userRepository).findAvailableAdmins();
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void testFindAvailableAdminAndAssignNoAvailableAdmins() {
        when(userRepository.findAvailableAdmins()).thenReturn(Arrays.asList());

        assertThrows(IllegalStateException.class, () -> 
            chatRoomService.findAvailableAdminAndAssign("room_12345678")
        );
    }

    @Test
    void testFindByRoomId() {
        when(chatRoomRepository.findByRoomId("room_12345678")).thenReturn(Optional.of(chatRoom));

        Optional<ChatRoom> result = chatRoomService.findByRoomId("room_12345678");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(chatRoom);
        verify(chatRoomRepository).findByRoomId("room_12345678");
    }

    @Test
    void testEndChatRoom() {
        chatRoom.setStatus(ChatRoom.RoomStatus.ACTIVE);
        chatRoom.setAdmin(admin1);
        chatRoom.setStartedAt(LocalDateTime.now().minusMinutes(10));
        
        when(chatRoomRepository.findByRoomId("room_12345678")).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(chatRoom);

        ChatRoom result = chatRoomService.endChatRoom("room_12345678");

        assertThat(result).isNotNull();
        verify(chatRoomRepository).findByRoomId("room_12345678");
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void testIsUserInRoom() {
        chatRoom.setAdmin(admin1);
        when(chatRoomRepository.findByRoomId("room_12345678")).thenReturn(Optional.of(chatRoom));

        boolean customerInRoom = chatRoomService.isUserInRoom("room_12345678", "customer1");
        boolean adminInRoom = chatRoomService.isUserInRoom("room_12345678", "admin1");
        boolean strangerInRoom = chatRoomService.isUserInRoom("room_12345678", "stranger");

        assertThat(customerInRoom).isTrue();
        assertThat(adminInRoom).isTrue();
        assertThat(strangerInRoom).isFalse();
    }

    @Test
    void testWaitingQueueOperations() {
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(0);

        chatRoomService.addToWaitingQueue("room_1");
        chatRoomService.addToWaitingQueue("room_2");
        
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(2);

        String nextRoom = chatRoomService.getNextWaitingCustomer();
        assertThat(nextRoom).isEqualTo("room_1");
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(1);

        chatRoomService.removeFromWaitingQueue("room_2");
        assertThat(chatRoomService.getWaitingQueueSize()).isEqualTo(0);
    }
}