package com.ezlevup.dentalchat.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/websocket-docs")
@Tag(name = "WebSocket Documentation", description = "WebSocket 엔드포인트 문서화 (실제 연결은 /ws 엔드포인트 사용)")
@Hidden
public class WebSocketDocController {

    @PostMapping("/chat/sendMessage/{roomId}")
    @Operation(
        summary = "채팅 메시지 전송 (WebSocket)",
        description = "WebSocket을 통해 특정 채팅방에 메시지를 전송합니다.\n\n" +
            "**실제 사용법:**\n" +
            "- WebSocket 연결: `/ws`\n" +
            "- 메시지 전송: `STOMP.send('/app/chat.sendMessage/{roomId}', {}, JSON.stringify(message))`\n" +
            "- 구독: `STOMP.subscribe('/topic/room/{roomId}')`"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 전송 성공",
            content = @Content(schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(value = "{\n" +
                "  \"content\": \"안녕하세요!\",\n" +
                "  \"sender\": \"customer1\",\n" +
                "  \"senderRole\": \"CUSTOMER\",\n" +
                "  \"type\": \"CHAT\",\n" +
                "  \"timestamp\": \"2024-01-01T12:00:00\",\n" +
                "  \"roomId\": \"room123\"\n" +
                "}")))
    })
    public ResponseEntity<Map<String, Object>> sendMessage(
        @Parameter(description = "채팅방 ID", required = true) @PathVariable String roomId,
        @Parameter(description = "채팅 메시지", required = true) @RequestBody Map<String, Object> message) {
        
        return ResponseEntity.ok(Map.of(
            "note", "이것은 문서화 목적의 엔드포인트입니다. 실제로는 WebSocket을 사용하세요.",
            "websocket_endpoint", "/ws",
            "stomp_destination", "/app/chat.sendMessage/" + roomId,
            "subscribe_topic", "/topic/room/" + roomId
        ));
    }

    @PostMapping("/chat/joinRoom/{roomId}")
    @Operation(
        summary = "채팅방 입장 (WebSocket)",
        description = "WebSocket을 통해 채팅방에 입장합니다.\n\n" +
            "**실제 사용법:**\n" +
            "- WebSocket 연결: `/ws`\n" +
            "- 채팅방 입장: `STOMP.send('/app/chat.joinRoom/{roomId}', {}, JSON.stringify(joinMessage))`\n" +
            "- 구독: `STOMP.subscribe('/topic/room/{roomId}')`"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 입장 성공",
            content = @Content(schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(value = "{\n" +
                "  \"content\": \"customer1 joined the room\",\n" +
                "  \"sender\": \"customer1\",\n" +
                "  \"senderRole\": \"CUSTOMER\",\n" +
                "  \"type\": \"JOIN\",\n" +
                "  \"timestamp\": \"2024-01-01T12:00:00\",\n" +
                "  \"roomId\": \"room123\"\n" +
                "}")))
    })
    public ResponseEntity<Map<String, Object>> joinRoom(
        @Parameter(description = "채팅방 ID", required = true) @PathVariable String roomId,
        @Parameter(description = "입장 메시지", required = true) @RequestBody Map<String, Object> joinMessage) {
        
        return ResponseEntity.ok(Map.of(
            "note", "이것은 문서화 목적의 엔드포인트입니다. 실제로는 WebSocket을 사용하세요.",
            "websocket_endpoint", "/ws",
            "stomp_destination", "/app/chat.joinRoom/" + roomId,
            "subscribe_topic", "/topic/room/" + roomId
        ));
    }

    @GetMapping("/endpoints")
    @Operation(
        summary = "WebSocket 엔드포인트 정보",
        description = "사용 가능한 WebSocket 엔드포인트와 사용법을 제공합니다."
    )
    public ResponseEntity<Map<String, Object>> getWebSocketEndpoints() {
        return ResponseEntity.ok(Map.of(
            "websocket_url", "/ws",
            "endpoints", Map.of(
                "sendMessage", Map.of(
                    "destination", "/app/chat.sendMessage/{roomId}",
                    "subscribe", "/topic/room/{roomId}",
                    "description", "채팅방에 메시지 전송"
                ),
                "joinRoom", Map.of(
                    "destination", "/app/chat.joinRoom/{roomId}",
                    "subscribe", "/topic/room/{roomId}",
                    "description", "채팅방 입장"
                )
            ),
            "message_types", Map.of(
                "CHAT", "일반 채팅 메시지",
                "JOIN", "채팅방 입장 메시지",
                "LEAVE", "채팅방 퇴장 메시지"
            ),
            "user_roles", Map.of(
                "CUSTOMER", "고객",
                "ADMIN", "관리자"
            )
        ));
    }
}