package com.ezlevup.dentalchat.controller;

import com.ezlevup.dentalchat.entity.User.UserType;
import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.service.ChatRoomService;
import com.ezlevup.dentalchat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        logger.info("관리자 대시보드 접속");
        
        List<ChatRoom> waitingRooms = chatRoomService.findWaitingRooms();
        int waitingQueueSize = chatRoomService.getWaitingQueueSize();
        
        model.addAttribute("waitingRooms", waitingRooms);
        model.addAttribute("waitingQueueSize", waitingQueueSize);
        
        return "admin/dashboard";
    }

    @GetMapping("/api/waiting-customers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getWaitingCustomers() {
        try {
            List<ChatRoom> waitingRooms = chatRoomService.findWaitingRooms();
            int queueSize = chatRoomService.getWaitingQueueSize();
            
            Map<String, Object> response = Map.of(
                "waitingRooms", waitingRooms,
                "queueSize", queueSize,
                "timestamp", System.currentTimeMillis()
            );
            
            logger.debug("대기 고객 목록 조회: {} 개", waitingRooms.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("대기 고객 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "대기 고객 목록을 불러올 수 없습니다."));
        }
    }

    @GetMapping("/api/active-rooms/{adminUsername}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActiveRooms(@PathVariable String adminUsername) {
        try {
            Optional<User> admin = userService.findByUsername(adminUsername);
            if (admin.isEmpty() || admin.get().getUserType() != UserType.ADMIN) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "유효하지 않은 관리자입니다."));
            }
            
            List<ChatRoom> activeRooms = chatRoomService.findActiveRoomsByAdmin(admin.get());
            
            Map<String, Object> response = Map.of(
                "activeRooms", activeRooms,
                "roomCount", activeRooms.size(),
                "timestamp", System.currentTimeMillis()
            );
            
            logger.debug("관리자 {} 활성 채팅방 조회: {} 개", adminUsername, activeRooms.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("활성 채팅방 조회 중 오류 발생: adminUsername={}", adminUsername, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "활성 채팅방 목록을 불러올 수 없습니다."));
        }
    }

    @PostMapping("/api/assign-customer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignCustomerToAdmin(
            @RequestBody Map<String, String> request) {
        try {
            String roomId = request.get("roomId");
            String adminUsername = request.get("adminUsername");
            
            if (roomId == null || adminUsername == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "roomId와 adminUsername이 필요합니다."));
            }
            
            Optional<User> admin = userService.findByUsername(adminUsername);
            if (admin.isEmpty() || admin.get().getUserType() != UserType.ADMIN) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "유효하지 않은 관리자입니다."));
            }
            
            ChatRoom assignedRoom = chatRoomService.assignAdmin(roomId, admin.get());
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "고객이 성공적으로 배정되었습니다.",
                "roomId", roomId,
                "adminUsername", adminUsername,
                "room", assignedRoom
            );
            
            logger.info("고객-관리자 매칭 완료: roomId={}, admin={}", roomId, adminUsername);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("고객 배정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("고객 배정 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "고객 배정 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/process-next-customer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processNextCustomer() {
        try {
            ChatRoom processedRoom = chatRoomService.processNextWaitingCustomer();
            
            if (processedRoom == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "대기 중인 고객이 없습니다."
                ));
            }
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "다음 고객이 자동으로 배정되었습니다.",
                "room", processedRoom
            );
            
            logger.info("다음 고객 자동 배정 완료: roomId={}", processedRoom.getRoomId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.warn("다음 고객 처리 실패: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("다음 고객 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "다음 고객 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/end-chat/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endChatRoom(@PathVariable String roomId) {
        try {
            ChatRoom endedRoom = chatRoomService.endChatRoom(roomId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "채팅방이 성공적으로 종료되었습니다.",
                "roomId", roomId,
                "room", endedRoom
            );
            
            logger.info("채팅방 종료 완료: roomId={}", roomId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("채팅방 종료 실패: roomId={}, error={}", roomId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("채팅방 종료 중 오류 발생: roomId={}", roomId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "채팅방 종료 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/api/room-details/{roomId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable String roomId) {
        try {
            Optional<ChatRoom> room = chatRoomService.findByRoomId(roomId);
            
            if (room.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = Map.of(
                "room", room.get(),
                "isExpired", chatRoomService.isSessionExpired(roomId),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("채팅방 상세 정보 조회 중 오류 발생: roomId={}", roomId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "채팅방 정보를 불러올 수 없습니다."));
        }
    }
}