package kr.hhplus.be.server.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.hhplus.be.server.common.kafka.event.KafkaEventPublisher;
import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.concert.controller.handler.RankingInfo;
import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.repository.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.concert.service.RankingService;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.application.service.redis.PurchaseTicketRedisServiceImpl;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.service.TicketDomainService;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import kr.hhplus.be.server.user.domain.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class KafkaIntegrationTest {

    @MockitoBean
    private TicketDomainService ticketDomainService;

    @MockitoBean
    private TicketRepositoryImpl ticketRepository;

    @MockitoBean
    private ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RedisCacheTemplate redisCacheTemplate;

    @Autowired
    private PurchaseTicketRedisServiceImpl purchaseTicketRedisService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ✅ Consumer가 메시지를 받았는지 확인용
    private final CountDownLatch latch = new CountDownLatch(1);
    private ConcertSoldOutEvent receivedEvent;

    // 공통 테스트 데이터
    private Long userId;
    private Long concertScheduleId;
    private Long ticketId;
    private BigDecimal useAmount;
    private PurchaseTicketCommandDto.Request request;
    private UserEntity mockUser;
    private Ticket mockTicket;
    private Ticket savedTicket;
    private Long concertId;
    private ConcertScheduleEntity mockConcertScheduleEntity;
    private ConcertScheduleEntity savedConcertScheduleEntity;

    @BeforeEach
    void setUp() {
        // 기본 테스트 데이터 설정
        concertId = IdUtils.getNewId();
        userId = IdUtils.getNewId();
        concertScheduleId = IdUtils.getNewId();
        ticketId = IdUtils.getNewId();
        useAmount = new BigDecimal("50000");

        // Request 객체 생성
        request = PurchaseTicketCommandDto.Request.builder()
                .userId(userId)
                .concertScheduleId(concertScheduleId)
                .ticketId(ticketId)
                .useAmount(useAmount)
                .build();

        // Mock 객체들 생성
        mockUser = UserEntity.builder()
                .userId(userId)
                .pointAmount(new BigDecimal("100000"))
                .build();

        mockTicket = new Ticket(
                ticketId, userId, 1L, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.RESERVED,
                null, useAmount, null, LocalDateTime.now().plusMinutes(5), userId
        );

        savedTicket = new Ticket(
                ticketId, userId, 1L, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.PAID,
                LocalDateTime.now(), useAmount, null,  LocalDateTime.now().plusMinutes(5), userId
        );

        mockConcertScheduleEntity = new ConcertScheduleEntity(
                concertScheduleId, 1L, 1L, LocalDate.of(2025,8,8),
                LocalTime.now(), LocalTime.now().plusHours(2), 100, CommonStatusEnum.ON_SELLING,
                null, null, null
        );

        savedConcertScheduleEntity = new ConcertScheduleEntity(
                concertScheduleId, 1L, 1L, LocalDate.of(2025,8,8),
                LocalTime.now(), LocalTime.now().plusHours(2), 100, CommonStatusEnum.SOLD_OUT,
                LocalDateTime.now(), null, null
        );

        // Redis 초기화
        String remainingKey = RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId);
        redisCacheTemplate.delete(remainingKey);

        String rankingKey = RedisKeyUtils.getDailyRankingKey();
        redisCacheTemplate.delete(rankingKey);

        //키 미리 등록 테스트
        redisCacheTemplate.set(RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId), 1L); // 매진 예정

    }

    // ✅ 실제 Kafka Consumer - 메시지 수신 확인용
    @KafkaListener(topics = "concert-soldout-events", groupId = "test-verification-group-unique")
    public void handleSoldOutEvent(String payload) {
        try {
            log.info("=== Kafka Consumer가 메시지 수신! ===");
            log.info("수신된 payload: {}", payload);

            receivedEvent = objectMapper.readValue(payload, ConcertSoldOutEvent.class);
            log.info("파싱된 이벤트: {}", receivedEvent);

            // ✅ 실제 랭킹 서비스 호출
            rankingService.addToRanking(
                    receivedEvent.concertScheduleId(),
                    "실제 카프카 테스트 콘서트",
                    receivedEvent.soldOutDatetime()
            );

            log.info("랭킹 서비스 호출 완료!");
            latch.countDown();  // 테스트 대기 해제

        } catch (Exception e) {
            log.error("Kafka 이벤트 처리 실패", e);
        }
    }


    @Test
    @DisplayName("실제 Kafka를 통한 완전한 이벤트 플로우 테스트")
    void realKafkaEventFlowTest() throws Exception {
        log.info("=== 실제 Kafka 통합 테스트 시작 ===");

        // Given - Mock 설정
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        when(concertScheduleJpaRepository.findById(any())).thenReturn(Optional.of(mockConcertScheduleEntity));

        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        doNothing().when(ticketDomainService).useUserPoint(mockUser, useAmount);

        // Redis 상태 확인
        String remainingKey = RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId);
        assertThat(redisCacheTemplate.get(remainingKey, Long.class)).isEqualTo(1L);
        log.info("Redis 매진 예정 상태 확인 완료: 1개 남음");

        // When - 실제 구매 실행 (실제 Kafka로 메시지 전송됨!)
        log.info("=== 티켓 구매 실행 (실제 Kafka 이벤트 전송) ===");
        PurchaseTicketCommandDto.Response response = purchaseTicketRedisService.purchase(request);

        // Then - 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        assertThat(response.isSuccess()).isTrue();
        log.info("구매 응답 검증 완료");

        // ✅ Kafka Consumer가 메시지를 받을 때까지 대기
        log.info("=== Kafka Consumer 메시지 수신 대기 (최대 15초) ===");
        boolean eventReceived = latch.await(15, TimeUnit.SECONDS);

        if (!eventReceived) {
            log.error("❌ 매진 이벤트 수신 실패");
        }

        // Kafka 이벤트 수신 검증
        Assertions.assertTrue(eventReceived, "15초 내에 Kafka 이벤트가 수신되지 않음!");
        Assertions.assertNotNull(receivedEvent, "수신된 이벤트가 null입니다!");
        Assertions.assertEquals(concertScheduleId, receivedEvent.concertScheduleId());
        log.info("✅ Kafka 이벤트 수신 및 파싱 성공!");
    }
}
