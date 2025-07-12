package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.ticket.application.port.in.ReserveTicketUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("예약 방식 성능 비교 테스트")
public class ReservationPerformanceComparisonTest {

    @Autowired
    @Qualifier("reserveTicketService")
    private ReserveTicketUseCase pessimisticLockService;

    @Autowired
    @Qualifier("reserveTicketAtomicService")
    private ReserveTicketUseCase atomicUpdateService;

    @Autowired
    private TicketRepository ticketRepository;

    private List<TicketEntity> testTickets;
    private final int TICKET_COUNT = 50;

    @TestConfiguration
    static class PerformanceTestConfig {

        @Bean
        @Primary
        public ReserveTicketUseCase pessimisticLockService(TicketRepository ticketRepository) {
            return new ReserveTicketServiceImpl(ticketRepository);
        }

        @Bean
        public ReserveTicketUseCase atomicUpdateService(TicketRepository ticketRepository) {
            return new ReserveTicketAtomicServiceImpl(ticketRepository);
        }
    }

    @BeforeEach
    @Transactional
    void setup() {
        // 기존 데이터 정리
        ticketRepository.deleteAll();

        // 테스트용 티켓 50개 생성
        testTickets = IntStream.range(0, TICKET_COUNT)
                .mapToObj(i -> TicketEntity.builder()
                        .seatId((long) (i + 1))
                        .concertScheduleId(1L)
                        .ticketNo("TKT" + String.format("%03d", i + 1))
                        .concertInfo("테스트 콘서트")
                        .seatInfo("좌석 " + (i + 1))
                        .ticketStatusEnum(TicketStatusEnum.AVAILABLE)
                        .totalAmount(BigDecimal.valueOf(10000))
                        .build())
                .toList();

        ticketRepository.saveAll(testTickets);
    }

    @Test
    @DisplayName("비관적 락 방식 성능 테스트 (100명 동시 접속)")
    void pessimistic_lock_performance_test() throws InterruptedException {
        System.out.println("\n 비관적 락 방식 성능 테스트 시작");

        PerformanceResult result = executePerformanceTest(
                pessimisticLockService,
                100,  // 동시 사용자 수
                "비관적 락"
        );

        printDetailedResult("비관적 락", result);

        // 기본 검증
        assertThat(result.getSuccessCount()).isGreaterThan(0);
        assertThat(result.getSuccessCount()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("조건부 UPDATE 방식 성능 테스트 (100명 동시 접속)")
    void atomic_update_performance_test() throws InterruptedException {
        System.out.println("\n 조건부 UPDATE 방식 성능 테스트 시작");

        PerformanceResult result = executePerformanceTest(
                atomicUpdateService,
                100,  // 더 많은 동시 사용자
                "조건부 UPDATE"
        );

        printDetailedResult("조건부 UPDATE", result);

        // 기본 검증
        assertThat(result.getSuccessCount()).isGreaterThan(0);
        assertThat(result.getSuccessCount()).isLessThanOrEqualTo(100); // 티켓 수 제한
    }

    @Test
    @DisplayName("직접 성능 비교 테스트 (동일 조건)")
    void direct_performance_comparison() throws InterruptedException {
        System.out.println("\n 두 방식 직접 성능 비교 시작");

        int concurrentUsers = 50; // 동일한 조건으로 비교

        // 1. 비관적 락 방식 테스트
        resetTicketsToAvailable();
        System.out.println("\n1 비관적 락 방식 테스트 중...");
        PerformanceResult pessimisticResult = executePerformanceTest(
                pessimisticLockService, concurrentUsers, "비관적 락");

        // 2. 조건부 UPDATE 방식 테스트
        resetTicketsToAvailable();
        System.out.println("\n2 조건부 UPDATE 방식 테스트 중...");
        PerformanceResult atomicResult = executePerformanceTest(
                atomicUpdateService, concurrentUsers, "조건부 UPDATE");

        // 3. 결과 비교 및 출력
        printComparisonResult(pessimisticResult, atomicResult);

        // 4. 성능 검증
        System.out.println("\n 성능 검증");
        System.out.println("조건부 UPDATE가 더 빠른지 확인: " +
                (atomicResult.getTotalExecutionTime() < pessimisticResult.getTotalExecutionTime()));

        // 조건부 UPDATE가 더 빨라야 함
        assertThat(atomicResult.getTotalExecutionTime())
                .describedAs("조건부 UPDATE가 비관적 락보다 빨라야 함")
                .isLessThan(pessimisticResult.getTotalExecutionTime());
    }

    private PerformanceResult executePerformanceTest(ReserveTicketUseCase service,
                                                     int concurrentUsers,
                                                     String testName) throws InterruptedException {
        System.out.println("테스트 시행 :: " + testName);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch startLatch = new CountDownLatch(1); // 초기값: 1 → "아직 출발하지 마!"
        CountDownLatch finishLatch = new CountDownLatch(concurrentUsers);

        // 결과 수집용
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        // 동시 요청 준비
        for (int i = 0; i < concurrentUsers; i++) {
            final Long userId = (long) (i + 1000); // 사용자 ID
            final Long ticketId = testTickets.get(i % TICKET_COUNT).getTicketId(); // 티켓 순환 선택

            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 동시 시작 대기

                    long startTime = System.currentTimeMillis();

                    try {
                        ReserveTicketCommandDto.Request request = createRequest(ticketId, userId);
                        service.reserve(request);
                        successCount.incrementAndGet();

                    } catch (ParameterNotValidException e) {
                        failureCount.incrementAndGet();
                        errorMessages.add("사용자 " + userId + ": " + e.getMessage());
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        errorMessages.add("사용자 " + userId + ": " + e.getClass().getSimpleName());
                    } finally {
                        long endTime = System.currentTimeMillis();
                        responseTimes.add(endTime - startTime);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 테스트 실행
        long overallStartTime = System.currentTimeMillis();
        startLatch.countDown(); // 🚀 모든 스레드 동시 시작!
        finishLatch.await(30, TimeUnit.SECONDS); // 최대 30초 대기
        long overallEndTime = System.currentTimeMillis();

        executor.shutdown();
if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
    executor.shutdownNow();
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        System.err.println("ExecutorService가 강제 종료되지 않았습니다.");
    }
}

        // 결과 반환
        return new PerformanceResult(
                overallEndTime - overallStartTime,
                successCount.get(),
                failureCount.get(),
                responseTimes,
                errorMessages
        );
    }

    private void printDetailedResult(String testName, PerformanceResult result) {
        System.out.println("\n📈 " + testName + " 상세 결과");
        System.out.println("------------------------------------------------");
        System.out.printf("⏱️  총 실행 시간: %d ms%n", result.getTotalExecutionTime());
        System.out.printf("✅ 성공한 예약: %d개%n", result.getSuccessCount());
        System.out.printf("❌ 실패한 예약: %d개%n", result.getFailureCount());
        System.out.printf("📊 성공률: %.1f%%%n", result.getSuccessRate());
        System.out.printf("⚡ 평균 응답시간: %.1f ms%n", result.getAverageResponseTime());
        System.out.printf("🏃 최소 응답시간: %d ms%n", result.getMinResponseTime());
        System.out.printf("🐌 최대 응답시간: %d ms%n", result.getMaxResponseTime());

        // 백분위수 출력
        System.out.printf("📊 응답시간 P50: %d ms, P95: %d ms, P99: %d ms%n",
                result.getPercentile(50), result.getPercentile(95), result.getPercentile(99));

        // 처음 5개 에러 메시지만 출력
        if (!result.getErrorMessages().isEmpty()) {
            System.out.println("\n🚨 주요 에러 메시지 (최대 5개):");
            result.getErrorMessages().stream().limit(5)
                    .forEach(msg -> System.out.println("   " + msg));
        }
    }

    private void printComparisonResult(PerformanceResult pessimistic, PerformanceResult atomic) {
        System.out.println("\n🏆 성능 비교 결과");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        System.out.printf("%-20s | %-15s | %-15s | %-10s%n", "항목", "비관적 락", "조건부 UPDATE", "개선율");
        System.out.println("─".repeat(70));

        // 총 실행 시간 비교
        double timeImprovement = (double) pessimistic.getTotalExecutionTime() / atomic.getTotalExecutionTime();
        System.out.printf("%-20s | %-15d | %-15d | %.1fx%n",
                "총 실행시간(ms)", pessimistic.getTotalExecutionTime(), atomic.getTotalExecutionTime(), timeImprovement);

        // 평균 응답시간 비교
        double avgResponseImprovement = pessimistic.getAverageResponseTime() / atomic.getAverageResponseTime();
        System.out.printf("%-20s | %-15.1f | %-15.1f | %.1fx%n",
                "평균 응답시간(ms)", pessimistic.getAverageResponseTime(), atomic.getAverageResponseTime(), avgResponseImprovement);

        // 성공률 비교
        System.out.printf("%-20s | %-15.1f | %-15.1f | --%n",
                "성공률(%)", pessimistic.getSuccessRate(), atomic.getSuccessRate());

        // 최대 응답시간 비교
        double maxResponseImprovement = (double) pessimistic.getMaxResponseTime() / atomic.getMaxResponseTime();
        System.out.printf("%-20s | %-15d | %-15d | %.1fx%n",
                "최대 응답시간(ms)", pessimistic.getMaxResponseTime(), atomic.getMaxResponseTime(), maxResponseImprovement);

        System.out.println("\n 결론:");
        System.out.printf("   • 조건부 UPDATE가 총 실행시간에서 %.1fx 빠름%n", timeImprovement);
        System.out.printf("   • 조건부 UPDATE가 평균 응답시간에서 %.1fx 빠름%n", avgResponseImprovement);
        System.out.printf("   • 조건부 UPDATE가 최대 응답시간에서 %.1fx 빠름%n", maxResponseImprovement);
    }

    private void resetTicketsToAvailable() {
        // 모든 티켓을 사용가능 상태로 초기화
        testTickets.forEach(ticket -> {
            ticket.setTicketStatusEnum(TicketStatusEnum.AVAILABLE);
            ticket.setReservedBy(null);
            ticket.setReservedUntil(null);
        });
        ticketRepository.saveAll(testTickets);
    }

    private ReserveTicketCommandDto.Request createRequest(Long ticketId, Long userId) {
        ReserveTicketCommandDto.Request request = new ReserveTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        return request;
    }

    @Data
    @AllArgsConstructor
    private static class PerformanceResult {
        private long totalExecutionTime;
        private int successCount;
        private int failureCount;
        private List<Long> responseTimes;
        private List<String> errorMessages;

        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total > 0 ? (double) successCount / total * 100 : 0;
        }

        public double getAverageResponseTime() {
            return responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }

        public long getMinResponseTime() {
            return responseTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
        }

        public long getMaxResponseTime() {
            return responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        }

        public long getPercentile(int percentile) {
            List<Long> sorted = responseTimes.stream().sorted().toList();
            if (sorted.isEmpty()) return 0L;
            int index = (int) Math.ceil(sorted.size() * percentile / 100.0) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }
    }

}
