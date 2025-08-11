package com.ezlevup.dentalchat.controller;

import com.ezlevup.dentalchat.entity.User.UserType;
import com.ezlevup.dentalchat.entity.ChatRoom;
import com.ezlevup.dentalchat.entity.User;
import com.ezlevup.dentalchat.service.ChatRoomService;
import com.ezlevup.dentalchat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "관리자 대시보드 및 채팅방 관리 API")
@SecurityRequirement(name = "basicAuth")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    @Operation(summary = "관리자 대시보드", description = "관리자 대시보드 페이지를 제공합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 대시보드 페이지를 반환")
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
    @Operation(summary = "대기 고객 목록 조회", description = "현재 상담을 기다리는 고객들의 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 대기 고객 목록을 반환",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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
    @Operation(summary = "관리자 활성 채팅방 조회", description = "특정 관리자가 담당하는 활성 채팅방 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 활성 채팅방 목록을 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 유효하지 않은 관리자"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getActiveRooms(
        @Parameter(description = "관리자 사용자명", required = true) @PathVariable String adminUsername) {
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
    @Operation(summary = "고객-관리자 배정", description = "대기 중인 고객을 특정 관리자에게 배정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 고객을 관리자에게 배정"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 필수 파라미터 누락 또는 유효하지 않은 관리자"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> assignCustomerToAdmin(
            @Parameter(description = "배정 요청 정보 (roomId, adminUsername)", required = true)
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
    @Operation(summary = "다음 고객 자동 배정", description = "대기열에서 다음 고객을 자동으로 배정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 다음 고객을 배정하거나 대기 고객이 없음을 반환"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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
    @Operation(summary = "채팅방 종료", description = "지정된 채팅방을 종료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 채팅방을 종료"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 유효하지 않은 채팅방"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> endChatRoom(
        @Parameter(description = "종료할 채팅방 ID", required = true) @PathVariable String roomId) {
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
    @Operation(summary = "채팅방 상세 정보 조회", description = "지정된 채팅방의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 채팅방 상세 정보를 반환"),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getRoomDetails(
        @Parameter(description = "조회할 채팅방 ID", required = true) @PathVariable String roomId) {
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