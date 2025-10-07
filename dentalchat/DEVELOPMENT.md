# 개발 가이드 (Development Guide)

## 프로젝트 개요

실시간 기반 치과 병원 고객 상담 채팅 시스템으로, Spring Boot 3.5.4와 Java 21을 기반으로 구축된 웹 애플리케이션입니다.

### 기술 스택
- **Framework**: Spring Boot 3.5.4
- **Java Version**: 21 (가상 스레드 지원)
- **Build Tool**: Maven
- **Database**: H2 (인메모리, 개발용)
- **Template Engine**: Thymeleaf
- **WebSocket**: STOMP 프로토콜
- **Security**: Spring Security
- **API Documentation**: SpringDoc OpenAPI 3
- **Frontend**: Bootstrap 5.3.2, jQuery 3.7.1, SockJS, STOMP

## 로컬 개발 환경 구성

### 필수 요구사항
- **Java 21** 이상
- **Maven 3.6+** (또는 프로젝트 내 Maven Wrapper 사용)
- **IDE**: IntelliJ IDEA, Eclipse, VS Code 등

### 1. 프로젝트 클론 및 설정
```bash
git clone [repository-url]
cd dental-clinic-chat/dentalchat
```

### 2. 빌드 및 실행
```bash
# 프로젝트 빌드
./mvnw clean compile

# 애플리케이션 실행
./mvnw spring-boot:run

# 테스트 실행
./mvnw test

# 패키징
./mvnw clean package
```

### 3. 애플리케이션 접속
- **메인 애플리케이션**: http://localhost:8080
- **H2 데이터베이스 콘솔**: http://localhost:8080/h2-console
- **API 문서**: http://localhost:8080/swagger-ui.html

### 4. H2 데이터베이스 연결 정보
- **JDBC URL**: `jdbc:h2:mem:dentalchat`
- **사용자명**: `sa`
- **비밀번호**: (공백)

## 코드 구조 및 패키지 설명

### 전체 패키지 구조
```
com.ezlevup.dentalchat/
├── DentalchatApplication.java          # 메인 애플리케이션 클래스
├── config/                             # 설정 클래스
│   ├── SecurityConfig.java             # Spring Security 설정
│   ├── SwaggerConfig.java              # API 문서 설정
│   ├── WebSocketConfig.java            # WebSocket 설정
│   └── WebSocketSecurityConfig.java    # WebSocket 보안 설정
├── controller/                         # 컨트롤러 계층
│   ├── AdminController.java            # 관리자 기능 컨트롤러
│   ├── AuthController.java             # 인증 관련 컨트롤러
│   ├── ChatController.java             # 채팅 기능 컨트롤러
│   ├── HomeController.java             # 메인 페이지 컨트롤러
│   └── WebSocketDocController.java     # WebSocket 문서화 컨트롤러
├── dto/                                # 데이터 전송 객체
│   ├── ChatMessage.java                # 채팅 메시지 DTO
│   ├── ChatRoomDto.java                # 채팅방 DTO
│   ├── MessageType.java                # 메시지 타입 enum
│   └── UserRole.java                   # 사용자 역할 enum
├── entity/                             # 엔티티 클래스 (JPA)
│   ├── ChatRoom.java                   # 채팅방 엔티티
│   ├── ChatSession.java                # 채팅 세션 엔티티
│   ├── Message.java                    # 메시지 엔티티
│   └── User.java                       # 사용자 엔티티
├── repository/                         # 데이터 접근 계층
│   ├── ChatRoomRepository.java         # 채팅방 레포지토리
│   ├── MessageRepository.java          # 메시지 레포지토리
│   └── UserRepository.java             # 사용자 레포지토리
└── service/                           # 비즈니스 로직 계층
    ├── ChatRoomService.java            # 채팅방 서비스
    ├── MessageService.java             # 메시지 서비스
    └── UserService.java                # 사용자 서비스
```

### 계층별 역할

#### 1. Controller 계층
- HTTP 요청/응답 처리
- WebSocket 메시지 처리 
- 요청 데이터 검증
- 응답 데이터 변환

#### 2. Service 계층
- 비즈니스 로직 구현
- 트랜잭션 관리
- 데이터 변환 및 검증
- 외부 서비스 연동

#### 3. Repository 계층
- 데이터베이스 CRUD 작업
- 커스텀 쿼리 구현
- JPA 기능 활용

#### 4. Entity 계층
- 데이터베이스 테이블 매핑
- 연관관계 정의
- 제약조건 설정

#### 5. DTO 계층
- 계층 간 데이터 전송
- API 요청/응답 형식 정의
- 데이터 캡슐화

## 새로운 기능 추가 방법

### 1. 새로운 엔티티 추가
```java
// 1. Entity 클래스 생성
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 필드, getter/setter, 연관관계 정의
}

// 2. Repository 인터페이스 생성
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // 커스텀 쿼리 메서드
}

// 3. Service 클래스 생성
@Service
@Transactional
public class AppointmentService {
    // 비즈니스 로직 구현
}

// 4. Controller 클래스 생성
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    // API 엔드포인트 구현
}
```

### 2. 새로운 WebSocket 엔드포인트 추가
```java
// WebSocketConfig.java에 엔드포인트 추가
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/appointments")
            .withSockJS();
}

// Controller에 메시지 핸들러 추가
@MessageMapping("/appointment.request")
@SendTo("/topic/appointments")
public AppointmentMessage handleAppointmentRequest(AppointmentMessage message) {
    // 예약 요청 처리 로직
    return message;
}
```

### 3. 새로운 설정 추가
```yaml
# application.yml에 커스텀 설정 추가
dental-chat:
  appointment:
    max-advance-days: 30
    working-hours:
      start: "09:00"
      end: "18:00"
```

```java
// 설정 클래스 생성
@ConfigurationProperties(prefix = "dental-chat.appointment")
@Component
public class AppointmentProperties {
    private int maxAdvanceDays;
    private WorkingHours workingHours;
    // getter/setter
}
```

## 테스트 실행 및 작성 가이드

### 테스트 실행 명령어
```bash
# 전체 테스트 실행
./mvnw test

# 특정 테스트 클래스 실행
./mvnw test -Dtest=ChatControllerTest

# 특정 테스트 메서드 실행
./mvnw test -Dtest=ChatControllerTest#shouldCreateChatRoom
```

### 단위 테스트 작성 예시

#### Service 테스트
```java
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @InjectMocks
    private ChatRoomService chatRoomService;
    
    @Test
    void shouldCreateChatRoom() {
        // Given
        ChatRoomDto dto = new ChatRoomDto();
        dto.setTitle("Test Room");
        
        ChatRoom savedRoom = new ChatRoom();
        savedRoom.setId(1L);
        savedRoom.setTitle("Test Room");
        
        when(chatRoomRepository.save(any(ChatRoom.class)))
            .thenReturn(savedRoom);
        
        // When
        ChatRoom result = chatRoomService.createChatRoom(dto);
        
        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Room");
    }
}
```

#### Controller 테스트
```java
@WebMvcTest(ChatController.class)
class ChatControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ChatRoomService chatRoomService;
    
    @Test
    void shouldReturnChatRoomList() throws Exception {
        // Given
        List<ChatRoom> rooms = Arrays.asList(
            new ChatRoom(1L, "Room 1"),
            new ChatRoom(2L, "Room 2")
        );
        
        when(chatRoomService.findAllChatRooms())
            .thenReturn(rooms);
        
        // When & Then
        mockMvc.perform(get("/api/chat/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title", is("Room 1")));
    }
}
```

#### WebSocket 테스트
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    
    @BeforeEach
    void setup() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        
        stompSession = stompClient.connect(
            String.format("ws://localhost:%d/ws", port),
            new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
    }
    
    @Test
    void shouldReceiveChatMessage() throws Exception {
        // Given
        BlockingQueue<ChatMessage> result = new LinkedBlockingDeque<>();
        
        stompSession.subscribe("/topic/chat", new DefaultStompFrameHandler(result));
        
        ChatMessage message = new ChatMessage();
        message.setContent("Hello World");
        message.setType(MessageType.CHAT);
        
        // When
        stompSession.send("/app/chat.sendMessage", message);
        
        // Then
        ChatMessage received = result.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.getContent()).isEqualTo("Hello World");
    }
}
```

## 데이터베이스 스키마 변경 방법

### 1. 개발 환경에서의 스키마 변경
현재 설정에서는 `ddl-auto: create-drop`으로 설정되어 있어 애플리케이션 재시작 시 스키마가 자동으로 재생성됩니다.

### 2. 엔티티 수정 시 주의사항
```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 새 컬럼 추가 시
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;
    
    // 기존 컬럼 수정 시
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
}
```

### 3. 데이터베이스 초기 데이터 추가
```java
// data.sql 파일을 resources 폴더에 생성
INSERT INTO users (username, email, role) VALUES 
('admin', 'admin@dental.com', 'ADMIN'),
('doctor1', 'doctor1@dental.com', 'DOCTOR');

// 또는 @PostConstruct를 사용한 데이터 초기화
@Component
public class DataInitializer {
    
    @Autowired
    private UserRepository userRepository;
    
    @PostConstruct
    public void initData() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }
    }
}
```

### 4. 프로덕션 환경을 위한 마이그레이션 준비
```yaml
# application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 또는 none
    
# Flyway 또는 Liquibase 사용 권장
# dependencies에 추가:
# <dependency>
#   <groupId>org.flywaydb</groupId>
#   <artifactId>flyway-core</artifactId>
# </dependency>
```

## 성능 최적화 팁

### 1. JPA 최적화

#### N+1 문제 해결
```java
// 잘못된 예시
@OneToMany(mappedBy = "chatRoom")
private List<Message> messages;

// 개선된 예시 - Fetch Join 사용
@Query("SELECT cr FROM ChatRoom cr JOIN FETCH cr.messages WHERE cr.id = :id")
ChatRoom findByIdWithMessages(@Param("id") Long id);

// 또는 @EntityGraph 사용
@EntityGraph(attributePaths = {"messages"})
ChatRoom findByIdWithMessages(Long id);
```

#### 배치 처리 최적화
```java
// 배치 삽입 최적화
@Modifying
@Query("INSERT INTO Message (content, chatRoom, user, timestamp) " +
       "VALUES (:content, :chatRoom, :user, :timestamp)")
void insertMessageBatch(@Param("content") String content,
                       @Param("chatRoom") ChatRoom chatRoom,
                       @Param("user") User user,
                       @Param("timestamp") LocalDateTime timestamp);
```

#### 페이징 처리
```java
// Repository에서 페이징 지원
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId, Pageable pageable);
}

// Service에서 사용
public Page<Message> getChatMessages(Long chatRoomId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return messageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable);
}
```

### 2. 캐시 활용

#### Spring Cache 사용
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

#### 커스텀 캐시 설정
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList("users", "chatrooms", "messages"));
        return cacheManager;
    }
}
```

### 3. WebSocket 최적화

#### 메시지 크기 제한
```java
// WebSocketConfig.java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic")
          .setMessageSizeLimit(64 * 1024)  // 64KB
          .setSendBufferSizeLimit(512 * 1024);  // 512KB
}
```

#### 연결 관리 최적화
```java
@Component
public class WebSocketEventListener {
    
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        int currentConnections = connectionCount.incrementAndGet();
        logger.info("새로운 WebSocket 연결. 총 연결 수: {}", currentConnections);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        int currentConnections = connectionCount.decrementAndGet();
        logger.info("WebSocket 연결 해제. 총 연결 수: {}", currentConnections);
    }
}
```

### 4. Java 21 가상 스레드 활용

#### 가상 스레드 설정 확인
```yaml
# application.yml에서 가상 스레드 활성화
spring:
  threads:
    virtual:
      enabled: true
```

#### 비동기 처리 최적화
```java
@Service
public class NotificationService {
    
    @Async
    public CompletableFuture<Void> sendNotificationAsync(String message) {
        // 가상 스레드에서 실행되는 비동기 알림 발송
        return CompletableFuture.runAsync(() -> {
            // 알림 발송 로직
        });
    }
}
```

### 5. 데이터베이스 최적화

#### 인덱스 추가
```java
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_chatroom_timestamp", 
           columnList = "chat_room_id, timestamp"),
    @Index(name = "idx_message_user", 
           columnList = "user_id")
})
public class Message {
    // 엔티티 필드
}
```

#### 커넥션 풀 튜닝
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      maximum-pool-size: 10
      minimum-idle: 2
```

## 프로파일별 환경 설정

### Development (dev)
- H2 콘솔 활성화
- SQL 로그 출력
- 디버그 로그 레벨

### Production (prod)  
- H2 콘솔 비활성화
- SQL 로그 비활성화
- WARN 로그 레벨

### 프로파일 전환
```bash
# 개발 환경 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 프로덕션 환경 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## 문제 해결 가이드

### 1. 일반적인 문제들

#### 포트 충돌 해결
```bash
# 포트 사용 중인 프로세스 확인 (Windows)
netstat -ano | findstr :8080

# 프로세스 종료
taskkill /PID [PID번호] /F
```

#### H2 데이터베이스 연결 실패
- JDBC URL 확인: `jdbc:h2:mem:dentalchat`
- 애플리케이션이 실행 중인지 확인
- H2 콘솔 경로 확인: `http://localhost:8080/h2-console`

#### WebSocket 연결 실패
- SockJS fallback 옵션 확인
- 브라우저 개발자 도구에서 네트워크 탭 확인
- CORS 설정 확인

### 2. 로그 분석
```bash
# 애플리케이션 로그 모니터링
tail -f logs/application.log

# 특정 패키지 로그만 확인
grep "com.ezlevup.dentalchat" logs/application.log
```

## 추가 도구 및 플러그인

### IDE 설정

#### IntelliJ IDEA 권장 플러그인
- Lombok
- Spring Boot Helper
- JPA Buddy
- Database Navigator

#### VS Code 권장 확장
- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support

### 코드 품질 도구
```xml
<!-- pom.xml에 추가 권장 -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.0</version>
</plugin>

<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
</plugin>
```

## 참고 자료

### 공식 문서
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring WebSocket Reference](https://docs.spring.io/spring-framework/reference/web/websocket.html)

### 추가 학습 자료
- [Spring Boot Best Practices](https://spring.io/guides)
- [JPA Performance Tuning](https://vladmihalcea.com/tutorials/hibernate/)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)

---

이 가이드는 새로운 개발자들이 프로젝트에 빠르게 적응하고 효율적으로 개발할 수 있도록 돕기 위해 작성되었습니다. 
추가 질문이나 개선 사항이 있다면 프로젝트 팀에 문의해주세요.