package kr.hhplus.be.server.ticket.application.integration;


import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.application.service.redis.ReserveTicketRedisServiceImpl;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("동시성 티켓 예약 테스트")
class ConcurrentTicketReservationRedisTest {

    @MockitoBean
    private TicketRepositoryImpl ticketRepository;

    @Autowired
    private ReserveTicketRedisServiceImpl reserveTicketRedisService;

    private List<Ticket> tickets;
    private List<Long> userIds;
    private final int TICKET_COUNT = 10;
    private final int USER_COUNT = 20; // 티켓보다 많은 사용자
    private Long concertScheduleId;
    private Long seatIdBase;

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
                    "콘서트 정보", "좌석 " + (i + 1), TicketStatusEnum.AVAILABLE,
                    null, new BigDecimal("10000"), null, null, null
            );
            tickets.add(ticket);
        }

        // 20명의 사용자 ID 생성
        userIds = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            userIds.add(IdUtils.getNewId());
        }
    }

    @Test
    @DisplayName("실제 DistributedLockTemplate을 이용한 동시성 테스트")
    void test_distributed_lock_with_real_redis() throws InterruptedException {

        // given
        Ticket ticket = tickets.get(0);

        // Repository Mock 설정
        when(ticketRepository.findById(ticket.getTicketId()))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when - 동시에 10명이 같은 티켓 예약 시도
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            Long userId = (long) (i + 1);
            executor.submit(() -> {
                try {
                    ReserveTicketCommandDto.Request request =
                            createReserveRequest(ticket.getTicketId(), userId);

                    reserveTicketRedisService.reserve(request);
                    successCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예약 성공");

                } catch (ParameterNotValidException e) {
                    // 락 획득 실패 (이미 예약됨)
                    failureCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예약 실패: " + e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("ExecutorService가 강제 종료되지 않았습니다.");
            }
        }

        // then - 분산 락이 정상 동작하면 1명만 성공해야 함
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get());
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);

    }

    @Test
    @DisplayName("동시성 환경에서 티켓 예약 - 성공/실패 케이스 혼재")
    void concurrent_ticket_reservation_with_random_selection() throws InterruptedException {
        // given
        setupMockForConcurrentTest();

        ExecutorService executorService = Executors.newFixedThreadPool(USER_COUNT);
        CountDownLatch latch = new CountDownLatch(USER_COUNT);

        // 결과 수집용
        ConcurrentHashMap<Long, Long> reservationResults = new ConcurrentHashMap<>(); // userId -> ticketId
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ConcurrentHashMap<Long, String> failureReasons = new ConcurrentHashMap<>();

        // when - 20명의 사용자가 동시에 랜덤 티켓 예약 시도
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    boolean reserved = attemptRandomTicketReservation(userId, reservationResults, failureReasons);
                    if (reserved) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await();
        executorService.shutdown();

        // then - 결과 검증
        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("성공한 예약: " + successCount.get());
        System.out.println("실패한 예약: " + failureCount.get());
        System.out.println("전체 시도: " + USER_COUNT);

        // 기본 검증
        assertThat(successCount.get()).isLessThanOrEqualTo(TICKET_COUNT); // 티켓 개수를 초과할 수 없음
        assertThat(successCount.get() + failureCount.get()).isEqualTo(USER_COUNT); // 모든 시도가 집계됨

        // 상세 결과 출력
        printDetailedResults(reservationResults, failureReasons);

        // 티켓별 예약 상태 확인
        verifyTicketReservationStates(reservationResults);
    }

    private ReserveTicketCommandDto.Request createReserveRequest(Long ticketId, Long userId) {
        ReserveTicketCommandDto.Request request = new ReserveTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        return request;
    }

    private void setupMockForConcurrentTest() {
        // 티켓 조회 시 실제 티켓 객체 반환
        when(ticketRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long ticketId = invocation.getArgument(0);
            return tickets.stream()
                    .filter(ticket -> ticket.getTicketId().equals(ticketId))
                    .findFirst();
        });

        // save 호출 시 아무것도 하지 않음 (Mock)
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private boolean attemptRandomTicketReservation(Long userId,
                                                   ConcurrentHashMap<Long, Long> reservationResults,
                                                   ConcurrentHashMap<Long, String> failureReasons) {
        List<Ticket> availableTickets = new ArrayList<>(tickets);
        Collections.shuffle(availableTickets); // 랜덤 순서

        for (Ticket ticket : availableTickets) {
            try {
                ReserveTicketCommandDto.Request request = createReserveRequest(ticket.getTicketId(), userId);
                ReserveTicketCommandDto.Response response = reserveTicketRedisService.reserve(request);

                if (response.getTicketId() != null) {
                    reservationResults.put(userId, ticket.getTicketId());
                    System.out.printf("사용자 %d가 티켓 %d 예약 성공%n", userId, ticket.getTicketId());
                    return true;
                }
            } catch (ParameterNotValidException e) {
                // 이미 예약된 티켓이면 다음 티켓 시도
                continue;
            } catch (Exception e) {
                failureReasons.put(userId, e.getMessage());
                break;
            }
        }

        failureReasons.put(userId, "예약 가능한 티켓이 없음");
        System.out.printf("사용자 %d 예약 실패: %s%n", userId, failureReasons.get(userId));
        return false;
    }

    private void printDetailedResults(ConcurrentHashMap<Long, Long> reservationResults,
                                      ConcurrentHashMap<Long, String> failureReasons) {
        System.out.println("\n=== 예약 성공 목록 ===");
        reservationResults.forEach((userId, ticketId) ->
                System.out.printf("사용자 %d -> 티켓 %d%n", userId, ticketId));

        System.out.println("\n=== 예약 실패 목록 ===");
        failureReasons.forEach((userId, reason) ->
                System.out.printf("사용자 %d -> %s%n", userId, reason));
    }

    private void verifyTicketReservationStates(ConcurrentHashMap<Long, Long> reservationResults) {
        System.out.println("\n=== 티켓별 최종 상태 ===");

        for (Ticket ticket : tickets) {
            boolean isReserved = reservationResults.containsValue(ticket.getTicketId());
            System.out.printf("티켓 %d: %s (예약자: %s)%n",
                    ticket.getTicketId(),
                    ticket.getTicketStatus(),
                    ticket.getReservedBy() != null ? ticket.getReservedBy() : "없음");

            if (isReserved) {
                assertThat(ticket.getTicketStatus()).isEqualTo(TicketStatusEnum.RESERVED);
                assertThat(ticket.getReservedBy()).isNotNull();
            }
        }
    }
}
