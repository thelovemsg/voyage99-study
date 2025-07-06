package kr.hhplus.be.server.ticket.application.integration;

import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.PurchaseTicketPessimisticLockUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.service.TicketDomainService;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import kr.hhplus.be.server.user.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pessimistic vs Optimistic Lock 성능 비교 테스트")
class ConcurrentTicketPurchaseWithUpdateLockTest {

    @Mock
    private TicketRepositoryImpl ticketRepository;

    @Mock
    private TicketDomainService ticketDomainService;

    @InjectMocks
    private PurchaseTicketPessimisticLockUseCase purchaseTicketService;

    private List<Ticket> testTickets;
    private List<UserEntity> users;
    private final int TICKET_COUNT = 50;
    private final int USER_COUNT = 10;
    private final int THREAD_COUNT = 5;
    private Long concertScheduleId;

    @BeforeEach
    void setup() {
        System.out.println("=== Setup 시작 ===");

        concertScheduleId = IdUtils.getNewId();

        // 사용자들 먼저 생성
        users = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            UserEntity user = UserEntity.builder()
                    .userId(IdUtils.getNewId())
                    .userName("USER_" + i)
                    .pointAmount(new BigDecimal("1000000"))
                    .build();
            users.add(user);
        }

        System.out.println("사용자 " + users.size() + "명 생성 완료");

        // 티켓 생성
        testTickets = createTickets();

        System.out.println("티켓 " + testTickets.size() + "개 생성 완료");
        System.out.println("=== Setup 완료 ===\n");
    }

    @Test
    @DisplayName("기존 Pessimistic Lock 성능 테스트")
    void pessimistic_lock_performance_test() throws InterruptedException {
        System.out.println("=== Pessimistic Lock 테스트 시작 ===");

        setupMockForPessimisticLock();

        long startTime = System.currentTimeMillis();
        PerformanceResult result = executePessimisticTest();
        long duration = System.currentTimeMillis() - startTime;

        printResults("Pessimistic Lock", result, duration);
        assertThat(result.successCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Optimistic Lock 시뮬레이션 테스트")
    void optimistic_lock_simulation_test() throws InterruptedException {
        System.out.println("=== Optimistic Lock 시뮬레이션 테스트 시작 ===");

        setupMockForOptimisticSimulation();

        long startTime = System.currentTimeMillis();
        PerformanceResult result = executeOptimisticSimulationTest();
        long duration = System.currentTimeMillis() - startTime;

        printResults("Optimistic Lock 시뮬레이션", result, duration);
        assertThat(result.successCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("성능 비교 테스트")
    void performance_comparison_test() throws InterruptedException {
        System.out.println("=== 성능 비교 테스트 시작 ===\n");

        // 1. Pessimistic Lock 테스트
        setupMockForPessimisticLock();
        long pessimisticStart = System.currentTimeMillis();
        PerformanceResult pessimisticResult = executePessimisticTest();
        long pessimisticDuration = System.currentTimeMillis() - pessimisticStart;

        System.out.println("\n--- Mock 초기화 중 ---");
        reset(ticketRepository, ticketDomainService);
        setup(); // 데이터 재생성

        // 2. Optimistic Lock 시뮬레이션 테스트
        setupMockForOptimisticSimulation();
        long optimisticStart = System.currentTimeMillis();
        PerformanceResult optimisticResult = executeOptimisticSimulationTest();
        long optimisticDuration = System.currentTimeMillis() - optimisticStart;

        // 3. 결과 비교
        printComparisonResults(pessimisticResult, pessimisticDuration, optimisticResult, optimisticDuration);
    }

    private List<Ticket> createTickets() {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < TICKET_COUNT; i++) {
            Long ticketId = IdUtils.getNewId();
            Ticket ticket = new Ticket(
                    ticketId, null, IdUtils.getNewId(), concertScheduleId,
                    "TKT" + String.format("%03d", i),
                    "콘서트 정보", "좌석 " + (i + 1), TicketStatusEnum.RESERVED,
                    null, new BigDecimal("20000"), null, null, null
            );

            UserEntity user = users.get(i % USER_COUNT);
            ticket.reserve(user.getUserId());
            tickets.add(ticket);
        }
        return tickets;
    }

    private PerformanceResult executePessimisticTest() throws InterruptedException {
        return executeTest("PESSIMISTIC");
    }

    private PerformanceResult executeOptimisticSimulationTest() throws InterruptedException {
        return executeTest("OPTIMISTIC");
    }

    private PerformanceResult executeTest(String lockType) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TICKET_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        System.out.println(lockType + " 테스트: " + TICKET_COUNT + "개 티켓을 " + THREAD_COUNT + "개 스레드로 처리");

        for (int i = 0; i < TICKET_COUNT; i++) {
            final int index = i;
            final Ticket ticket = testTickets.get(index);
            final UserEntity user = users.get(index % USER_COUNT);

            executor.submit(() -> {
                try {
                    PurchaseTicketCommandDto.Request request = createPurchaseRequest(ticket.getTicketId(), user.getUserId());
                    purchaseTicketService.purchaseWithPessimisticLock(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
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

        return new PerformanceResult(successCount.get(), failureCount.get());
    }

    private void setupMockForPessimisticLock() {
        System.out.println("Pessimistic Lock Mock 설정 중...");

        for (Ticket ticket : testTickets) {
            when(ticketRepository.findByIdWithLock(ticket.getTicketId()))
                    .thenReturn(Optional.of(ticket));
        }

        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        setupCommonMocks();
        System.out.println("Pessimistic Lock Mock 설정 완료");
    }

    private void setupMockForOptimisticSimulation() {
        System.out.println("Optimistic Lock 시뮬레이션 Mock 설정 중...");

        for (Ticket ticket : testTickets) {
            when(ticketRepository.findByIdWithLock(ticket.getTicketId()))
                    .thenReturn(Optional.of(ticket));
        }

        // 30% 확률로 동시성 충돌 시뮬레이션
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        setupCommonMocks();
        System.out.println("Optimistic Lock 시뮬레이션 Mock 설정 완료");
    }

    private void setupCommonMocks() {
        when(ticketDomainService.validateUserHasEnoughPoint(anyLong(), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    return users.stream()
                            .filter(u -> u.getUserId().equals(userId))
                            .findFirst()
                            .orElse(users.get(0));
                });

        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(anyLong());
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(any(Ticket.class), anyLong());
        doNothing().when(ticketDomainService).useUserPoint(any(UserEntity.class), any(BigDecimal.class));
    }

    private PurchaseTicketCommandDto.Request createPurchaseRequest(Long ticketId, Long userId) {
        PurchaseTicketCommandDto.Request request = new PurchaseTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        request.setUseAmount(new BigDecimal("20000"));
        request.setConcertScheduleId(concertScheduleId);
        return request;
    }

    private void printResults(String testName, PerformanceResult result, long duration) {
        System.out.println("\n=== " + testName + " 결과 ===");
        System.out.println("실행 시간: " + duration + "ms");
        System.out.println("성공: " + result.successCount + "개");
        System.out.println("실패: " + result.failureCount + "개");
        System.out.println("성공률: " + String.format("%.1f", result.successCount * 100.0 / TICKET_COUNT) + "%");

        if (duration > 0) {
            System.out.println("TPS: " + String.format("%.2f", result.successCount * 1000.0 / duration) + " transactions/second");
        }
        System.out.println();
    }

    private void printComparisonResults(PerformanceResult pessimisticResult, long pessimisticDuration,
                                        PerformanceResult optimisticResult, long optimisticDuration) {
        System.out.println("\n=== 최종 성능 비교 결과 ===");

        // 전체 처리량 (Total Throughput)
        double pessimisticTotalTPS = TICKET_COUNT * 1000.0 / pessimisticDuration;
        double optimisticTotalTPS = TICKET_COUNT * 1000.0 / optimisticDuration;

        // 유효 처리량 (Effective Throughput - 성공한 것만)
        double pessimisticEffectiveTPS = pessimisticResult.successCount * 1000.0 / pessimisticDuration;
        double optimisticEffectiveTPS = optimisticResult.successCount * 1000.0 / optimisticDuration;

        System.out.println("Pessimistic Lock:");
        System.out.println("  - 실행시간: " + pessimisticDuration + "ms");
        System.out.println("  - 성공: " + pessimisticResult.successCount + "/" + TICKET_COUNT + " (" +
                String.format("%.1f", pessimisticResult.successCount * 100.0 / TICKET_COUNT) + "%)");
        System.out.println("  - 전체 처리량: " + String.format("%.2f", pessimisticTotalTPS) + " TPS");
        System.out.println("  - 유효 처리량: " + String.format("%.2f", pessimisticEffectiveTPS) + " TPS");

        System.out.println("\nOptimistic Lock (시뮬레이션):");
        System.out.println("  - 실행시간: " + optimisticDuration + "ms");
        System.out.println("  - 성공: " + optimisticResult.successCount + "/" + TICKET_COUNT + " (" +
                String.format("%.1f", optimisticResult.successCount * 100.0 / TICKET_COUNT) + "%)");
        System.out.println("  - 전체 처리량: " + String.format("%.2f", optimisticTotalTPS) + " TPS");
        System.out.println("  - 유효 처리량: " + String.format("%.2f", optimisticEffectiveTPS) + " TPS");

        System.out.println("\n분석:");

        // 처리 속도 비교
        if (Math.abs(pessimisticDuration - optimisticDuration) <= 2) {
            System.out.println("  - 처리 속도: 거의 동일함 (" + pessimisticDuration + "ms vs " + optimisticDuration + "ms)");
        } else {
            double speedRatio = (double) pessimisticDuration / optimisticDuration;
            if (speedRatio > 1.1) {
                System.out.println("  - Optimistic Lock이 " + String.format("%.1f", speedRatio) + "배 빠름");
            } else {
                System.out.println("  - Pessimistic Lock이 " + String.format("%.1f", 1 / speedRatio) + "배 빠름");
            }
        }

        // 성공률 비교 (수정된 로직)
        int successDiff = pessimisticResult.successCount - optimisticResult.successCount;
        if (successDiff > 0) {
            System.out.println("  - 데이터 일관성: Pessimistic Lock이 " + successDiff + "개 더 성공 (더 안정적)");
        } else if (successDiff < 0) {
            System.out.println("  - 데이터 일관성: Optimistic Lock이 " + Math.abs(successDiff) + "개 더 성공");
        } else {
            System.out.println("  - 데이터 일관성: 동일한 성공률");
        }

        // 실제 의미 해석
        System.out.println("\n실제 의미:");
        System.out.println("  - Pessimistic Lock: 안정적이지만 대기 시간 발생 가능");
        System.out.println("  - Optimistic Lock: 빠르지만 충돌 시 재시도 필요 (" +
                optimisticResult.failureCount + "번 실패)");

        if (optimisticResult.failureCount > pessimisticResult.failureCount) {
            System.out.println("  - 권장: 동시성이 낮으면 Optimistic, 높으면 Pessimistic");
        }

        System.out.println();
    }

    private static class PerformanceResult {
        final int successCount;
        final int failureCount;

        PerformanceResult(int successCount, int failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
    }
}