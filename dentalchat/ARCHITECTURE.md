# 실시간 치과 병원 고객 상담 채팅 시스템 아키텍처

## 1. 시스템 개요

이 시스템은 Spring Boot 3.5.4와 Java 21을 기반으로 구축된 실시간 치과 병원 고객 상담 채팅 시스템입니다. WebSocket과 STOMP 프로토콜을 활용하여 고객과 상담원 간의 실시간 양방향 통신을 제공합니다.

## 2. 시스템 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                         클라이언트 계층                          │
├─────────────────────────────────────────────────────────────────┤
│  Web Browser (고객/상담원)                                       │
│  ├─ HTML/CSS/JavaScript                                        │
│  ├─ SockJS Client                                             │
│  ├─ STOMP WebSocket Client                                    │
│  └─ Bootstrap UI Framework                                    │
└─────────────────────────────────────────────────────────────────┘
                                │
                          WebSocket/HTTP
                                │
┌─────────────────────────────────────────────────────────────────┐
│                          웹 계층                               │
├─────────────────────────────────────────────────────────────────┤
│  Spring Security Filter Chain                                  │
│  ├─ Authentication Filter                                     │
│  ├─ Authorization Filter                                      │
│  └─ CSRF Protection                                          │
│                                                               │
│  Controllers                                                  │
│  ├─ ChatController (WebSocket)                               │
│  ├─ HomeController (HTTP)                                    │
│  ├─ AdminController (HTTP)                                   │
│  └─ AuthController (HTTP)                                    │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │
┌─────────────────────────────────────────────────────────────────┐
│                        메시징 계층                             │
├─────────────────────────────────────────────────────────────────┤
│  Spring WebSocket/STOMP Configuration                          │
│  ├─ WebSocketConfig                                           │
│  ├─ Message Broker (/topic)                                  │
│  ├─ Application Destination (/app)                           │
│  └─ User Destination (/user)                                 │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │
┌─────────────────────────────────────────────────────────────────┐
│                         서비스 계층                            │
├─────────────────────────────────────────────────────────────────┤
│  Business Logic Services                                        │
│  ├─ ChatRoomService (채팅방 관리)                              │
│  ├─ MessageService (메시지 처리)                              │
│  ├─ UserService (사용자 관리)                                 │
│  └─ Scheduled Tasks (세션 타이머)                             │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │
┌─────────────────────────────────────────────────────────────────┐
│                        데이터 접근 계층                        │
├─────────────────────────────────────────────────────────────────┤
│  Spring Data JPA Repositories                                  │
│  ├─ ChatRoomRepository                                        │
│  ├─ MessageRepository                                         │
│  └─ UserRepository                                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │
┌─────────────────────────────────────────────────────────────────┐
│                        데이터베이스 계층                       │
├─────────────────────────────────────────────────────────────────┤
│  H2 In-Memory Database                                          │
│  ├─ users 테이블                                              │
│  ├─ chat_rooms 테이블                                         │
│  └─ messages 테이블                                           │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 데이터 플로우 설명

### 3.1 고객 접속 → 매칭 → 채팅 플로우

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   고객 접속   │    │  채팅방 생성  │    │  상담원 매칭  │    │   실시간 채팅  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
1. 고객이 홈페이지    →   2. 새 채팅방 생성    →   3. 대기열에 추가    →   4. WebSocket 연결
   에서 채팅 시작          (WAITING 상태)          및 상담원 자동배정       및 메시지 교환

┌──────────────────────────────────────────────────────────────────────────┐
│                          상세 프로세스                                     │
├──────────────────────────────────────────────────────────────────────────┤
│ 1단계: 고객 접속                                                         │
│   • HomeController를 통해 메인 페이지 로드                                │
│   • 익명 사용자로 채팅 시작 요청                                          │
│                                                                          │
│ 2단계: 채팅방 생성                                                       │
│   • ChatRoomService.createChatRoom() 호출                              │
│   • UUID 기반 고유 룸ID 생성 (room_xxxxxxxx)                            │
│   • 채팅방 상태: WAITING으로 설정                                        │
│   • 데이터베이스에 채팅방 정보 저장                                       │
│                                                                          │
│ 3단계: 상담원 매칭                                                       │
│   • 대기열(ConcurrentLinkedQueue)에 룸ID 추가                           │
│   • findAvailableAdminAndAssign() 호출                                 │
│   • 가용한 상담원 중 가장 적은 활성 세션을 가진 상담원 선택                 │
│   • 채팅방 상태: ACTIVE로 변경, 상담원 배정                              │
│   • 세션 타이머 시작 (30분 제한)                                         │
│                                                                          │
│ 4단계: 실시간 채팅                                                       │
│   • WebSocket 연결: /chat 엔드포인트                                     │
│   • STOMP 프로토콜을 통한 메시지 구독: /topic/room/{roomId}               │
│   • 메시지 발송: /app/chat.sendMessage/{roomId}                         │
│   • 실시간 양방향 통신 시작                                              │
└──────────────────────────────────────────────────────────────────────────┘
```

## 4. 주요 컴포넌트 역할 설명

### 4.1 엔티티 (Entity) 계층
- **User**: 사용자 정보 관리 (고객/상담원 구분, 온라인 상태)
- **ChatRoom**: 채팅방 정보 관리 (대기/활성/종료 상태, 참여자 관계)
- **Message**: 메시지 정보 관리 (내용, 발송자, 타입, 읽음 상태)

### 4.2 서비스 (Service) 계층
- **ChatRoomService**: 
  - 채팅방 생성, 상담원 매칭, 세션 관리
  - 대기열 관리 (ConcurrentLinkedQueue)
  - 자동 세션 만료 처리 (@Scheduled)
- **MessageService**: 메시지 저장, 조회, 상태 업데이트
- **UserService**: 사용자 관리, 온라인 상태 추적

### 4.3 컨트롤러 (Controller) 계층
- **ChatController**: WebSocket 메시지 핸들링
- **HomeController**: 웹 페이지 라우팅
- **AdminController**: 상담원 대시보드 관리

### 4.4 설정 (Configuration) 계층
- **WebSocketConfig**: STOMP WebSocket 설정
- **SecurityConfig**: 인증/인가, CSRF 보호
- **SwaggerConfig**: API 문서화

## 5. WebSocket 통신 구조

### 5.1 연결 구성
```yaml
WebSocket 엔드포인트: /chat
STOMP 구독: /topic/room/{roomId}
메시지 발송: /app/chat.sendMessage/{roomId}
참가 알림: /app/chat.joinRoom/{roomId}
```

### 5.2 메시지 타입
- **CHAT**: 일반 채팅 메시지
- **JOIN**: 채팅방 참여 알림
- **LEAVE**: 채팅방 나가기 알림
- **SYSTEM**: 시스템 메시지

### 5.3 통신 흐름
```
클라이언트                     서버                    타 클라이언트
    │                          │                          │
    ├─ WebSocket 연결 (/chat) ──▶ 연결 수락                  │
    │                          │                          │
    ├─ 구독 (/topic/room/xxx) ──▶ 구독 등록                  │
    │                          │                          │
    ├─ JOIN 메시지 (/app/...) ──▶ 메시지 처리 ──▶ 브로드캐스트 ──▶ JOIN 알림 수신
    │                          │                          │
    ├─ CHAT 메시지 (/app/...) ──▶ 메시지 처리 ──▶ 브로드캐스트 ──▶ 채팅 메시지 수신
    │                          │                          │
    ◀── 메시지 수신 ◀─────────── 브로드캐스트 ◀──── 타 클라이언트 메시지
```

## 6. 데이터베이스 스키마 설계

### 6.1 테이블 구조

#### Users 테이블
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    nickname VARCHAR(100) NOT NULL,
    user_type ENUM('CUSTOMER', 'ADMIN') NOT NULL,
    status ENUM('ONLINE', 'OFFLINE', 'BUSY') NOT NULL DEFAULT 'OFFLINE',
    created_at DATETIME NOT NULL,
    last_seen DATETIME
);
```

#### Chat_Rooms 테이블
```sql
CREATE TABLE chat_rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(100) NOT NULL UNIQUE,
    customer_id BIGINT,
    admin_id BIGINT,
    status ENUM('WAITING', 'ACTIVE', 'ENDED') NOT NULL DEFAULT 'WAITING',
    created_at DATETIME NOT NULL,
    started_at DATETIME,
    ended_at DATETIME,
    customer_notes VARCHAR(500),
    
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (admin_id) REFERENCES users(id)
);
```

#### Messages 테이블
```sql
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type ENUM('CHAT', 'SYSTEM', 'JOIN', 'LEAVE') NOT NULL DEFAULT 'CHAT',
    sent_at DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);
```

### 6.2 관계 설계
- **User ↔ ChatRoom**: One-to-Many (한 사용자는 여러 채팅방 참여 가능)
- **ChatRoom ↔ Message**: One-to-Many (한 채팅방에 여러 메시지 존재)
- **User ↔ Message**: One-to-Many (한 사용자는 여러 메시지 발송 가능)

### 6.3 인덱스 설계
```sql
-- 성능 최적화를 위한 인덱스
CREATE INDEX idx_chat_rooms_status ON chat_rooms(status);
CREATE INDEX idx_chat_rooms_customer_id ON chat_rooms(customer_id);
CREATE INDEX idx_chat_rooms_admin_id ON chat_rooms(admin_id);
CREATE INDEX idx_messages_chat_room_id ON messages(chat_room_id);
CREATE INDEX idx_messages_sent_at ON messages(sent_at);
```

## 7. 보안 구조 설명

### 7.1 인증 및 인가
```yaml
인증 방식: 
  - Spring Security 폼 로그인
  - BCrypt 패스워드 인코딩
  - 인메모리 사용자 저장소

권한 구조:
  - ROLE_ADMIN: 상담원 (관리자 페이지 접근)
  - ROLE_USER: 일반 사용자
  - 익명 사용자: 채팅만 가능

보호 경로:
  - /admin/**: ROLE_ADMIN 권한 필요
  - /api/admin/**: ROLE_ADMIN 권한 필요
  - /chat/**: 모든 사용자 허용 (익명 포함)
```

### 7.2 웹 보안 헤더
```yaml
보안 헤더:
  - X-Frame-Options: DENY
  - X-Content-Type-Options: nosniff
  - Strict-Transport-Security: max-age=31536000; includeSubDomains
  - Referrer-Policy: strict-origin-when-cross-origin

CSRF 보호:
  - 쿠키 기반 CSRF 토큰
  - WebSocket 엔드포인트 제외 (/chat/**)
```

### 7.3 세션 관리
```yaml
세션 설정:
  - 세션 타임아웃: 30분
  - 동시 세션 제한: 1개 (중복 로그인 방지)
  - 세션 고정 공격 방지: 새 세션 생성
```

### 7.4 WebSocket 보안
```yaml
WebSocket 보안:
  - SockJS 폴백 지원
  - Origin 검증: 모든 도메인 허용 (개발용)
  - 세션 기반 인증 유지
  - 메시지 유효성 검증 (길이 제한: 1000자)
```

## 8. 기술적 의사결정 및 구현 이유

### 8.1 기술 스택 선택

#### Spring Boot 3.5.4 + Java 21
**선택 이유:**
- 가상 스레드 지원으로 높은 동시성 처리 가능
- Spring의 성숙한 생태계와 풍부한 기능
- 자동 설정으로 빠른 개발 가능

#### H2 In-Memory 데이터베이스
**선택 이유:**
- 개발 및 테스트 환경에서 빠른 구동
- 설정 없이 즉시 사용 가능
- JPA 호환성 우수

**프로덕션 고려사항:**
- MySQL/PostgreSQL로 마이그레이션 필요
- 데이터 영속성 보장 필요

#### WebSocket + STOMP
**선택 이유:**
- HTTP보다 낮은 지연시간으로 실시간 통신 가능
- STOMP 프로토콜로 메시지 라우팅 간소화
- SockJS 폴백으로 브라우저 호환성 확보

### 8.2 아키텍처 설계 결정

#### 대기열 시스템 (ConcurrentLinkedQueue)
**선택 이유:**
- 멀티 스레드 환경에서 안전한 FIFO 큐 구현
- 메모리 기반으로 빠른 성능
- Lock-free 알고리즘으로 높은 동시성

**확장성 고려:**
- Redis 기반 분산 큐로 확장 가능
- 클러스터 환경 지원

#### 세션 타이머 시스템
**구현 이유:**
- 유휴 세션 자동 정리로 리소스 절약
- @Scheduled 어노테이션으로 간단한 구현
- ConcurrentHashMap으로 스레드 안전성 보장

#### 가상 스레드 활용
**선택 이유:**
- Java 21의 가상 스레드로 높은 동시성 처리
- 기존 스레드 모델 대비 메모리 사용량 감소
- I/O 작업이 많은 채팅 시스템에 최적

### 8.3 보안 설계 결정

#### 익명 사용자 채팅 허용
**선택 이유:**
- 고객 접근성 향상 (회원가입 없이 상담 가능)
- 빠른 상담 서비스 제공
- 치과 병원 특성상 즉시 상담 필요

#### 메시지 유효성 검증
**구현 이유:**
- 길이 제한 (1000자)으로 DoS 공격 방지
- 빈 메시지, 악성 입력 차단
- 사용자 경험 향상

### 8.4 성능 최적화 결정

#### JPA 최적화
```yaml
설정 최적화:
  - Batch Insert/Update: 20개 단위 처리
  - 연관관계 지연 로딩으로 N+1 문제 방지
  - 쿼리 로그 및 실행 계획 확인 가능

인덱스 설계:
  - 채팅방 상태별 조회 최적화
  - 메시지 시간순 정렬 최적화
```

#### HTTP/2 및 압축 활용
```yaml
성능 향상:
  - HTTP/2 활성화로 다중화 통신
  - Gzip 압축으로 대역폭 절약
  - 정적 리소스 캐싱
```

## 9. 확장성 고려사항

### 9.1 수평적 확장
- Redis 분산 세션 저장소 도입
- 메시지 브로커 (RabbitMQ/Apache Kafka) 활용
- 로드 밸런서를 통한 다중 인스턴스 운영

### 9.2 모니터링 및 관찰가능성
- Spring Boot Actuator를 통한 헬스체크
- 메트릭 수집 및 알림 시스템
- 로그 수집 및 분석 (ELK Stack)

### 9.3 데이터베이스 확장
- 읽기 전용 복제본 구성
- 샤딩을 통한 데이터 분산
- 메시지 아카이빙 시스템 도입