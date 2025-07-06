package kr.hhplus.be.server.ticket.application.integration;

import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.application.service.PurchaseTicketPessimisticLockServiceImpl;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.service.TicketDomainService;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import kr.hhplus.be.server.user.domain.UserEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
@DisplayName("동시성 티켓 구매 테스트")
class ConcurrentTicketPurchaseAmountLimitTest {

    @Mock
    private TicketRepositoryImpl ticketRepository;

    @Mock
    private TicketDomainService ticketDomainService;

    @InjectMocks
    private PurchaseTicketPessimisticLockServiceImpl purchaseTicketService;

    private List<Ticket> tickets;
    private List<UserEntity> users;
    private final int TICKET_COUNT = 50;
    private final int USER_COUNT = 2; // 티켓보다 많은 사용자
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
                    "콘서트 정보", "좌석 " + (i + 1), TicketStatusEnum.RESERVED,
                    null, new BigDecimal(20_000), null, null, null
            );
            tickets.add(ticket);
        }

        users = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            UserEntity userEntity = UserEntity.builder()
                    .userId(IdUtils.getNewId())
                    .userName("TESTER_" + i)
                    .pointAmount(new BigDecimal(100_000))
                    .build();

            users.add(userEntity);
        }
    }

    @Test
    void validate_test() {
        // Mock 객체인지 확인
        System.out.println("ticketDomainService class: " + ticketDomainService.getClass());
        System.out.println("Is Mock: " + Mockito.mockingDetails(ticketDomainService).isMock());

        // 직접 호출해서 테스트
        try {
            ticketDomainService.validateConcertScheduleAvailable(1L);
            System.out.println("Mock 설정 성공!");
        } catch (Exception e) {
            System.out.println("Mock 설정 실패: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("단순 동시성 테스트 - 디버깅용")
    void simple_concurrent_test() throws InterruptedException {
        // given - 1개 티켓만으로 테스트
        Ticket singleTicket = tickets.get(0);

        singleTicket.reserve(users.get(0).getUserId()); // 첫 번째 사용자로 예약

        // Repository Mock 설정
        when(ticketRepository.findByIdWithLock(singleTicket.getTicketId()))
                .thenReturn(Optional.of(singleTicket));
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 수정된 부분

        // 모든 Mock 설정을 한 번에
        UserEntity mockUser = createMockUser();

        when(ticketDomainService.validateUserHasEnoughPoint(anyLong(), any(BigDecimal.class))).thenReturn(mockUser);
        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(anyLong());
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(any(Ticket.class), anyLong());
        doNothing().when(ticketDomainService).useUserPoint(any(UserEntity.class), any(BigDecimal.class));

        // when - 2명만 동시에 시도
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            UserEntity userEntity = users.get(i);
            Long userId = userEntity.getUserId();
            executor.submit(() -> {
                try {
                    PurchaseTicketCommandDto.Request request = createPurchaseRequest(singleTicket.getTicketId(), userId);
                    purchaseTicketService.purchaseWithPessimisticLock(request);
                    successCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예약 성공");
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예약 실패: " + e.getMessage());
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

        // then
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get());
        assertThat(successCount.get()).isEqualTo(1); // 1명만 성공해야 함
        assertThat(failureCount.get()).isEqualTo(1); // 1명은 실패해야 함

        System.out.println("userEntity = " + users.get(0));
        System.out.println("userEntity = " + users.get(1));
    }

    @Test
    @DisplayName("50장 티켓 2명이서 구매")
    void buy_10_tickets_by_users() throws InterruptedException {
        setupTicketsAndUsers();

        // Repository Mock 설정
        for (Ticket ticket : tickets) {
            when(ticketRepository.findByIdWithLock(ticket.getTicketId()))
                    .thenReturn(Optional.of(ticket));
        }
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 수정된 부분

        // 모든 Mock 설정을 한 번에
        UserEntity mockUser = createMockUser();

        when(ticketDomainService.validateUserHasEnoughPoint(anyLong(), any(BigDecimal.class))).thenReturn(mockUser);
        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(anyLong());
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(any(Ticket.class), anyLong());

        // when - 2명만 동시에 시도
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(50);  // ← 50개로 수정

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);
            UserEntity userEntity = i % 2 == 0 ? users.get(0) : users.get(1);
            Long userId = userEntity.getUserId();
            executor.submit(() -> {
                try {
                    PurchaseTicketCommandDto.Request request = createPurchaseRequest(ticket.getTicketId(), userId);
                    purchaseTicketService.purchaseWithPessimisticLock(request);
                    successCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예약 성공");
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 예약 실패: " + e.getMessage());
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

        // then
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get());
    }

    @Test
    @DisplayName("50장 티켓 2명이서 구매 - 포인트 한계 테스트")
    void buy_50_tickets_by_2_users_with_point_limit() throws InterruptedException {
        // given
        setupTicketsAndUsers();

        // when - 50개 작업 동시 실행

        setupMockSettings();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(50);  // ← 50개로 수정

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < tickets.size(); i++) {
            final int index = i;
            Ticket ticket = tickets.get(index);

            // 사용자 번갈아가며 선택 (25개씩)
            UserEntity userEntity = (index % 2 == 0) ? users.get(0) : users.get(1);
            Long userId = userEntity.getUserId();

            executor.submit(() -> {
                try {
                    PurchaseTicketCommandDto.Request request = createPurchaseRequest(ticket.getTicketId(), userId);
                    purchaseTicketService.purchaseWithPessimisticLock(request);
                    successCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 티켓 " + index + " 구매 성공");
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("사용자 " + userId + " 티켓 " + index + " 구매 실패: " + e.getMessage());
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

        // then
        System.out.println("성공: " + successCount.get() + ", 실패: " + failureCount.get());
        System.out.println("User1 남은 포인트: " + users.get(0).getPointAmount());
        System.out.println("User2 남은 포인트: " + users.get(1).getPointAmount());

        // 각 사용자는 5개씩 (100000원 ÷ 20000원), 총 10개 성공
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(40);
    }

    private void setupTicketsAndUsers() {
        for (int i = 0; i < tickets.size(); i++) {
            UserEntity userEntity = (i % 2 == 0) ? users.get(0) : users.get(1);
            tickets.get(i).reserve(userEntity.getUserId()); // 이 과정에서 ID가 바뀔 수 있음
        }
    }

    private PurchaseTicketCommandDto.@NotNull Request createPurchaseRequest(Long ticketId, Long userId) {
        PurchaseTicketCommandDto.Request request = new PurchaseTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        request.setUseAmount(new BigDecimal("20000")); // 이것도 추가해야 함
        request.setConcertScheduleId(concertScheduleId); // 이것도 추가
        return request;
    }

    private UserEntity createMockUser() {
        // Mock UserEntity 생성 (UserEntity 클래스에 맞게 수정)
        return UserEntity.builder()
                .userId(1L)
                .pointAmount(new BigDecimal("100000"))
                .build();
    }

    private void setupMockSettings() {
        // Repository Mock 설정
        for (Ticket ticket : tickets) {
            when(ticketRepository.findByIdWithLock(ticket.getTicketId()))
                    .thenReturn(Optional.of(ticket));
        }
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 실제 포인트 검증과 차감이 일어나도록 설정
        when(ticketDomainService.validateUserHasEnoughPoint(anyLong(), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    BigDecimal amount = invocation.getArgument(1);

                    // 해당 사용자 찾기
                    UserEntity user = users.stream()
                            .filter(u -> u.getUserId().equals(userId))
                            .findFirst()
                            .orElseThrow();

                    // 포인트 부족 시 예외 발생
                    if (user.getPointAmount().compareTo(amount) < 0) {
                        throw new RuntimeException("포인트 부족");
                    }

                    return user;
                });

        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            BigDecimal amount = invocation.getArgument(1);

            System.out.println("포인트 차감 전: " + user.getPointAmount());
            user.usePoint(amount);
            System.out.println("포인트 차감 후: " + user.getPointAmount());
            System.out.println("차감 금액: " + amount);
            return null;
        }).when(ticketDomainService).useUserPoint(any(UserEntity.class), any(BigDecimal.class));

        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(anyLong());
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(any(Ticket.class), anyLong());
    }
}
