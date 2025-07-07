package kr.hhplus.be.server.ticket.application.ticket.service.event;

import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import kr.hhplus.be.server.concert.repository.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.concert.service.handler.DataPlatformClient;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PurchaseTicketEventServiceImplTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private TicketDomainService ticketDomainService;

    @MockitoBean
    private TicketRepositoryImpl ticketRepository;

    @MockitoBean
    private ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @MockitoBean
    private RedisCacheTemplate redisCacheTemplate;

    @Autowired
    private PurchaseTicketRedisServiceImpl purchaseTicketRedisService;

    @MockitoBean
    private DataPlatformClient dataPlatformClient;

    @MockitoBean
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
    private ConcertTicketPurchaseEvent event;

    @BeforeEach
    void setUp() {
        log.info("테스트 서버 포트: {}", port); // 실제 포트 확인

        // 기본 테스트 데이터 설정
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

        event = ConcertTicketPurchaseEvent.fromTicketEntity(savedTicket);

        // Redis 초기화
        String remainingKey = RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId);
        redisCacheTemplate.delete(remainingKey);

        String rankingKey = RedisKeyUtils.getDailyRankingKey();
        redisCacheTemplate.delete(rankingKey);

        //키 미리 등록 테스트
        redisCacheTemplate.set(RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId), 1L); // 매진 예정

    }

    @Test
    @DisplayName("구매 -> 데이터 전송 이벤트 발생 테스트")
    void redisRankingAddTest() {
        // Given
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        when(concertScheduleJpaRepository.findById(any())).thenReturn(Optional.of(mockConcertScheduleEntity));

        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        doNothing().when(ticketDomainService).useUserPoint(mockUser, useAmount);

        String remainingKey = RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId);
        when(redisCacheTemplate.get(remainingKey, Object.class)).thenReturn(1L);

        doAnswer(invocation -> {
            dataPlatformClient.sendTicketPurchaseInfo(event);
            return null;
        }).when(eventPublisher).publishEvent(any(ConcertTicketPurchaseEvent.class));

//        // When
        PurchaseTicketCommandDto.Response response = purchaseTicketRedisService.purchase(request);

        doNothing().when(dataPlatformClient).sendTicketPurchaseInfo(any());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        assertThat(response.isSuccess()).isTrue();

        // 검증
        verify(ticketRepository).findById(ticketId);
        verify(ticketDomainService).validateUserHasEnoughPoint(userId, useAmount);
        verify(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        verify(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        verify(ticketDomainService).useUserPoint(mockUser, useAmount);
        verify(dataPlatformClient, timeout(1000)).sendTicketPurchaseInfo(any());

    }
}