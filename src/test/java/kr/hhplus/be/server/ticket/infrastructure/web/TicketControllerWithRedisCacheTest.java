package kr.hhplus.be.server.ticket.infrastructure.web;


import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.GetTicketCommandDto;
import kr.hhplus.be.server.ticket.application.service.GetTicketCacheServiceImpl;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketEntity;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketJpaRepository;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerWithRedisCacheTest {

    private final int TICKET_COUNT = 50;
    private List<Ticket> tickets;
    private Long concertScheduleId;
    private Long seatIdBase;

    @Autowired
    private GetTicketCacheServiceImpl getTicketService;

    @MockitoBean
    private TicketJpaRepository ticketJpaRepository;

    @Autowired
    private RedisCacheTemplate cacheTemplate;

    @BeforeEach
    void setup() {
        concertScheduleId = IdUtils.getNewId();
        seatIdBase = IdUtils.getNewId();

        // 10개의 티켓 생성
        tickets = new ArrayList<>();
        for (int i = 0; i < TICKET_COUNT; i++) {
            Long ticketId = IdUtils.getNewId();
            Ticket ticket = new Ticket(
                    ticketId, null, seatIdBase + i, concertScheduleId, "TKT" + String.format("%03d", i),
                    "콘서트 정보", "좌석 " + (i + 1), TicketStatusEnum.RESERVED,
                    null, new BigDecimal(20_000), null, null, null
            );
            tickets.add(ticket);
        }

    }

    @Test
    @DisplayName("캐시 미스 -> 캐시 히트 테스트")
    void testCacheHitAndMiss() {
        // Given
        Ticket ticket = tickets.get(0);
        Long ticketId = ticket.getTicketId();
        TicketEntity ticketEntity = TicketMapper.toEntity(ticket);
        Mockito.when(ticketJpaRepository.findById(ticketId)).thenReturn(Optional.ofNullable(ticketEntity));

        // When - 첫 번째 호출 (캐시 미스, DB 조회)
        GetTicketCommandDto.Response response1 = getTicketService.getTicket(ticketId);

        // When - 두 번째 호출 (캐시 히트, DB 조회하지 않음)
        GetTicketCommandDto.Response response2 = getTicketService.getTicket(ticketId);

        // Then
        Assertions.assertThat(response1.getTicketId()).isEqualTo(ticketId);
        Assertions.assertThat(response2.getTicketId()).isEqualTo(ticketId);

        // 중요: DB는 1번만 호출되어야 함
        verify(ticketJpaRepository, times(1)).findById(ticketId);
    }

    @Test
    @DisplayName("캐시 성능 비교 테스트 - DB vs Redis Cache")
    void testCachePerformanceComparison() {
        // Given
        Ticket ticket = tickets.get(0);
        Long ticketId = ticket.getTicketId();
        TicketEntity ticketEntity = TicketMapper.toEntity(ticket);

        // DB 조회 시뮬레이션 (약간의 지연 추가)
        Mockito.when(ticketJpaRepository.findById(ticketId))
                .thenAnswer(invocation -> {
                    Thread.sleep(10); // DB 조회 시뮬레이션 (10ms)
                    return Optional.of(ticketEntity);
                });

        // 캐시 초기화
        String cacheKey = "ticket:info:" + ticketId;
        cacheTemplate.delete(cacheKey);

        // When & Then
        log.info("=== 캐시 성능 테스트 시작 ===");

        // 1. 첫 번째 호출 (캐시 미스 - DB 조회)
        long startTime1 = System.nanoTime();
        GetTicketCommandDto.Response response1 = getTicketService.getTicket(ticketId);
        long dbCallTime = System.nanoTime() - startTime1;

        log.info("DB 조회 시간: {}ms ({}ns)", dbCallTime / 1_000_000.0, dbCallTime);

        // 2. 두 번째 호출 (캐시 히트)
        long startTime2 = System.nanoTime();
        GetTicketCommandDto.Response response2 = getTicketService.getTicket(ticketId);
        long cacheCallTime = System.nanoTime() - startTime2;

        log.info("캐시 조회 시간: {}ms ({}ns)", cacheCallTime / 1_000_000.0, cacheCallTime);

        // 성능 개선 비율 계산
        double speedupRatio = (double) dbCallTime / cacheCallTime;
        log.info("캐시 성능 개선: {}배 빠름", String.format("%.2f", speedupRatio));

        // 검증
        Assertions.assertThat(response1.getTicketId()).isEqualTo(ticketId);
        Assertions.assertThat(response2.getTicketId()).isEqualTo(ticketId);

        // 캐시가 더 빨라야 함
        Assertions.assertThat(cacheCallTime).isLessThan(dbCallTime);

        // DB는 1번만 호출
        verify(ticketJpaRepository, times(1)).findById(ticketId);
    }

    @Test
    @DisplayName("다중 티켓 조회 성능 테스트")
    void testMultipleTicketPerformance() {
        // Given
        List<Long> ticketIds = tickets.stream()
                .limit(10) // 처음 10개만
                .map(Ticket::getTicketId)
                .toList();

        // 모든 티켓에 대한 Mock 설정
        for (int i = 0; i < 10; i++) {
            Ticket ticket = tickets.get(i);
            TicketEntity entity = TicketMapper.toEntity(ticket);
            Mockito.when(ticketJpaRepository.findById(ticket.getTicketId()))
                    .thenAnswer(invocation -> {
                        Thread.sleep(5); // DB 조회 시뮬레이션
                        return Optional.of(entity);
                    });
        }

        log.info("=== 다중 티켓 조회 성능 테스트 ===");

        // When - 첫 번째 라운드 (모두 캐시 미스)
        long startTime1 = System.nanoTime();
        List<GetTicketCommandDto.Response> responses1 = new ArrayList<>();
        for (Long ticketId : ticketIds) {
            responses1.add(getTicketService.getTicket(ticketId));
        }
        long firstRoundTime = System.nanoTime() - startTime1;

        log.info("첫 번째 라운드 (DB 조회): {}ms", firstRoundTime / 1_000_000.0);

        // When - 두 번째 라운드 (모두 캐시 히트)
        long startTime2 = System.nanoTime();
        List<GetTicketCommandDto.Response> responses2 = new ArrayList<>();
        for (Long ticketId : ticketIds) {
            responses2.add(getTicketService.getTicket(ticketId));
        }
        long secondRoundTime = System.nanoTime() - startTime2;

        log.info("두 번째 라운드 (캐시 조회): {}ms", secondRoundTime / 1_000_000.0);

        // 성능 개선 비율
        double speedupRatio = (double) firstRoundTime / secondRoundTime;
        log.info("전체 성능 개선: {}배 빠름", String.format("%.2f", speedupRatio));

        // Then
        Assertions.assertThat(responses1).hasSize(10);
        Assertions.assertThat(responses2).hasSize(10);
        Assertions.assertThat(secondRoundTime).isLessThan(firstRoundTime);

        // 각 티켓마다 DB는 1번씩만 호출
        for (Long ticketId : ticketIds) {
            verify(ticketJpaRepository, times(1)).findById(ticketId);
        }
    }

}
