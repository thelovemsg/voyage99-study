package kr.hhplus.be.server.ticket.application.ticket.service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
import kr.hhplus.be.server.common.retry.model.EventTypeEnum;
import kr.hhplus.be.server.common.retry.model.OutboxEventEntity;
import kr.hhplus.be.server.common.retry.repository.OutboxEventRepository;
import kr.hhplus.be.server.common.retry.service.OutboxEventPublisher;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.concert.controller.handler.RankingInfo;
import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseTicketOutboxTest {

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

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // 공통 테스트 데이터
    private Long userId;
    private Long concertScheduleId;
    private Long ticketId;
    private BigDecimal useAmount;
    private PurchaseTicketCommandDto.Request request;
    private UserEntity mockUser;
    private Ticket mockTicket;
    private Ticket savedTicket;
    private ConcertScheduleEntity mockConcertScheduleEntity;

    @BeforeEach
    void setUp() {
        // 기본 테스트 데이터 설정
        userId = IdUtils.getNewId();
        concertScheduleId = IdUtils.getNewId();
        ticketId = IdUtils.getNewId();
        useAmount = new BigDecimal("50000");

        request = PurchaseTicketCommandDto.Request.builder()
                .userId(userId)
                .concertScheduleId(concertScheduleId)
                .ticketId(ticketId)
                .useAmount(useAmount)
                .build();

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
                LocalDateTime.now(), useAmount, null, LocalDateTime.now().plusMinutes(5), userId
        );

        mockConcertScheduleEntity = new ConcertScheduleEntity(
                concertScheduleId, 1L, 1L, LocalDate.of(2025, 8, 8),
                LocalTime.now(), LocalTime.now().plusHours(2), 100, CommonStatusEnum.ON_SELLING,
                null, null, null
        );

        // Redis 및 DB 초기화
        String remainingKey = RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId);
        redisCacheTemplate.delete(remainingKey);
        String rankingKey = RedisKeyUtils.getDailyRankingKey();
        redisCacheTemplate.delete(rankingKey);

        // Outbox 테이블 초기화
        outboxEventRepository.deleteAll();

        // Redis에 매진 예정 상태로 설정 (1개 남음)
        redisCacheTemplate.set(RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId), 1L);
    }

    @Test
    @DisplayName("티켓 구매 시 Outbox에 이벤트가 저장되는지 테스트")
    @Transactional
    void shouldSaveEventsToOutboxWhenPurchaseTicket() throws JsonProcessingException {
        // Given
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        when(concertScheduleJpaRepository.findById(any())).thenReturn(Optional.of(mockConcertScheduleEntity));

        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        doNothing().when(ticketDomainService).useUserPoint(mockUser, useAmount);

        // When
        PurchaseTicketCommandDto.Response response = purchaseTicketRedisService.purchase(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();

        // 1. Outbox에 이벤트가 저장되었는지 확인
        List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(2); // TICKET_PURCHASED + CONCERT_SOLD_OUT

        // 2. TICKET_PURCHASED 이벤트 검증
        OutboxEventEntity ticketEvent = outboxEvents.stream()
                .filter(event -> event.getEventType() == EventTypeEnum.TICKET_PURCHASED)
                .findFirst()
                .orElseThrow();

        assertThat(ticketEvent.getAggregateId()).isEqualTo(ticketId.toString());
        assertThat(ticketEvent.getProcessed()).isFalse();
        assertThat(ticketEvent.getRetryCount()).isEqualTo(0);

        // 페이로드 검증
        ConcertTicketPurchaseEvent deserializedEvent = objectMapper.readValue(
                ticketEvent.getPayload(), ConcertTicketPurchaseEvent.class);
        assertThat(deserializedEvent.getTicketId()).isEqualTo(ticketId);
        assertThat(deserializedEvent.getUserId()).isEqualTo(userId);

        // 3. CONCERT_SOLD_OUT 이벤트 검증
        OutboxEventEntity soldOutEvent = outboxEvents.stream()
                .filter(event -> event.getEventType() == EventTypeEnum.CONCERT_SOLD_OUT)
                .findFirst()
                .orElseThrow();

        assertThat(soldOutEvent.getAggregateId()).isEqualTo(concertScheduleId.toString());
        assertThat(soldOutEvent.getProcessed()).isFalse();

        ConcertSoldOutEvent deserializedSoldOutEvent = objectMapper.readValue(
                soldOutEvent.getPayload(), ConcertSoldOutEvent.class);
        assertThat(deserializedSoldOutEvent.concertScheduleId()).isEqualTo(concertScheduleId);

        log.info("✅ Outbox에 이벤트 저장 완료: {} 개", outboxEvents.size());
    }

    @Test
    @DisplayName("OutboxEventPublisher가 저장된 이벤트를 정상적으로 발행하는지 테스트")
    @Transactional
    void shouldPublishEventsFromOutbox() throws Exception {
        // Given: 먼저 Outbox에 이벤트를 저장
        shouldSaveEventsToOutboxWhenPurchaseTicket();

        // 이벤트 리스너 모킹을 위한 준비
        RankingService spyRankingService = spy(rankingService);

        // When: OutboxEventPublisher 실행
        outboxEventPublisher.publishPendingEvents();

        // Then:
        // 1. Outbox 이벤트들이 processed = true로 변경되었는지 확인
        List<OutboxEventEntity> processedEvents = outboxEventRepository.findAll();
        assertThat(processedEvents).hasSize(2);

        for (OutboxEventEntity event : processedEvents) {
            assertThat(event.getProcessed()).isTrue();
            assertThat(event.getProcessedAt()).isNotNull();
            log.info("✅ 이벤트 처리 완료: type={}, processed={}",
                    event.getEventType(), event.getProcessed());
        }

        // 2. 실제 이벤트가 발행되어 비즈니스 로직이 실행되었는지 확인
        // (RankingService가 호출되었는지는 별도 이벤트 리스너 테스트에서 확인)
        List<OutboxEventEntity> remainingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        assertThat(remainingEvents).isEmpty();

        log.info("✅ 모든 Outbox 이벤트 발행 완료");
    }

    @Test
    @DisplayName("이벤트 발행 실패 시 재시도 카운트가 증가하는지 테스트")
    @Transactional
    void shouldIncrementRetryCountOnFailure() {
        // Given: 강제로 실패하는 이벤트 생성
        OutboxEventEntity failingEvent = OutboxEventEntity.builder()
                .aggregateId("test-id")
                .eventType(EventTypeEnum.TICKET_PURCHASED)
                .payload("invalid-json-payload") // 잘못된 JSON으로 실패 유도
                .build();
        outboxEventRepository.save(failingEvent);

        Long originalEventId = failingEvent.getId();

        // When: 실패하는 이벤트 발행 시도
        outboxEventPublisher.publishPendingEvents();

        // Then: 재시도 카운트가 증가했는지 확인
        OutboxEventEntity updatedEvent = outboxEventRepository.findById(originalEventId)
                .orElseThrow();

        assertThat(updatedEvent.getProcessed()).isFalse();
        assertThat(updatedEvent.getRetryCount()).isEqualTo(1);
        assertThat(updatedEvent.canRetry()).isTrue();

        log.info("✅ 실패한 이벤트 재시도 카운트 증가: retryCount={}", updatedEvent.getRetryCount());
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 더 이상 처리하지 않는지 테스트")
    @Transactional
    void shouldStopRetryingAfterMaxAttempts() {
        // Given: 이미 최대 재시도 횟수를 초과한 이벤트 생성
        OutboxEventEntity maxRetriedEvent = OutboxEventEntity.builder()
                .aggregateId("max-retry-test")
                .eventType(EventTypeEnum.TICKET_PURCHASED)
                .payload("invalid-json")
                .build();

        // 최대 재시도 횟수만큼 증가
        for (int i = 0; i < 3; i++) {
            maxRetriedEvent.incrementRetry();
        }
        outboxEventRepository.save(maxRetriedEvent);

        assertThat(maxRetriedEvent.canRetry()).isFalse();

        // When: 발행 시도
        outboxEventPublisher.publishPendingEvents();

        // Then: 재시도 카운트가 더 이상 증가하지 않았는지 확인
        OutboxEventEntity updatedEvent = outboxEventRepository.findById(maxRetriedEvent.getId())
                .orElseThrow();

        assertThat(updatedEvent.getRetryCount()).isEqualTo(3); // 더 이상 증가하지 않음
        assertThat(updatedEvent.getProcessed()).isFalse();
        assertThat(updatedEvent.canRetry()).isFalse();

        log.info("✅ 최대 재시도 초과 이벤트는 더 이상 처리하지 않음: retryCount={}",
                updatedEvent.getRetryCount());
    }

    @Test
    @DisplayName("여러 이벤트 중 일부만 실패해도 성공한 이벤트는 정상 처리되는지 테스트")
    @Transactional
    void shouldProcessSuccessfulEventsEvenIfOthersFail() throws JsonProcessingException {
        // Given: 성공할 이벤트와 실패할 이벤트를 함께 저장

        // 성공할 이벤트
        ConcertTicketPurchaseEvent validEvent = ConcertTicketPurchaseEvent.builder()
                .ticketId(ticketId)
                .userId(userId)
                .concertScheduleId(concertScheduleId)
                .build();

        OutboxEventEntity successEvent = OutboxEventEntity.builder()
                .aggregateId("success-event")
                .eventType(EventTypeEnum.TICKET_PURCHASED)
                .payload(objectMapper.writeValueAsString(validEvent))
                .build();

        // 실패할 이벤트
        OutboxEventEntity failEvent = OutboxEventEntity.builder()
                .aggregateId("fail-event")
                .eventType(EventTypeEnum.CONCERT_SOLD_OUT)
                .payload("invalid-json")
                .build();

        outboxEventRepository.save(successEvent);
        outboxEventRepository.save(failEvent);

        // When: 발행 시도
        outboxEventPublisher.publishPendingEvents();

        // Then: 성공한 이벤트는 처리되고, 실패한 이벤트는 재시도 대상이 되었는지 확인
        OutboxEventEntity updatedSuccessEvent = outboxEventRepository.findById(successEvent.getId())
                .orElseThrow();
        OutboxEventEntity updatedFailEvent = outboxEventRepository.findById(failEvent.getId())
                .orElseThrow();

        // 성공한 이벤트는 processed = true
        assertThat(updatedSuccessEvent.getProcessed()).isTrue();
        assertThat(updatedSuccessEvent.getProcessedAt()).isNotNull();

        // 실패한 이벤트는 processed = false, retryCount 증가
        assertThat(updatedFailEvent.getProcessed()).isFalse();
        assertThat(updatedFailEvent.getRetryCount()).isEqualTo(1);

        log.info("✅ 성공한 이벤트: processed={}", updatedSuccessEvent.getProcessed());
        log.info("✅ 실패한 이벤트: retryCount={}", updatedFailEvent.getRetryCount());
    }
}