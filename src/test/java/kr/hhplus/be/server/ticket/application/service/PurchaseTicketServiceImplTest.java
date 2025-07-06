package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.exceptions.ReservationNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import kr.hhplus.be.server.ticket.domain.service.TicketDomainService;
import kr.hhplus.be.server.user.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseTicketServiceImpl 테스트")
class PurchaseTicketServiceImplTest {

    @Mock
    private TicketDomainService ticketDomainService;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private PurchaseTicketPessimisticLockServiceImpl purchaseTicketPessimisticLockService;

    // 공통 테스트 데이터
    private Long userId;
    private Long concertScheduleId;
    private Long ticketId;
    private BigDecimal useAmount;
    private PurchaseTicketCommandDto.Request request;
    private UserEntity mockUser;
    private Ticket mockTicket;
    private Ticket savedTicket;

    @BeforeEach
    void setUp() {
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
                ticketId, null, 1L, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.AVAILABLE,
                null, useAmount, null, null, null
        );

        savedTicket = new Ticket(
                ticketId, userId, 1L, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.RESERVED,
                null, useAmount, null,  LocalDateTime.now().plusMinutes(5), userId
        );
    }

    @Test
    @DisplayName("티켓 구매 성공 테스트")
    void purchase_WithPessimicticLock_Success() throws Exception {
        // Given
        when(ticketRepository.findByIdWithLock(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        doNothing().when(ticketDomainService).useUserPoint(mockUser, useAmount);

        // When
        PurchaseTicketCommandDto.Response response = purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        assertThat(response.isSuccess()).isTrue();

        // 검증
        verify(ticketRepository).findByIdWithLock(ticketId);
        verify(ticketDomainService).validateUserHasEnoughPoint(userId, useAmount);
        verify(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        verify(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        verify(ticketDomainService).useUserPoint(mockUser, useAmount);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("티켓을 찾을 수 없는 경우 예외 발생")
    void purchase_WithPessimicticLock_TicketNotFound_ThrowsException() {
        // Given
        when(ticketRepository.findByIdWithLock(ticketId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository).findByIdWithLock(ticketId);
        verifyNoInteractions(ticketDomainService);
    }

    @Test
    @DisplayName("사용자 포인트 부족으로 구매 실패")
    void purchase_WithPessimicticLock_InsufficientPoint_ThrowsException() {
        // Given
        when(ticketRepository.findByIdWithLock(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount))
                .thenThrow(new ParameterNotValidException(MessageCode.USER_POINT_NOT_ENOUGH));

        // When & Then
        assertThatThrownBy(() -> purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request))
                .isInstanceOf(ParameterNotValidException.class);

        verify(ticketRepository).findByIdWithLock(ticketId);
        verify(ticketDomainService).validateUserHasEnoughPoint(userId, useAmount);
        verify(ticketRepository, never()).save(any());
        verify(ticketDomainService, never()).useUserPoint(any(), any());
    }

    @Test
    @DisplayName("콘서트 스케줄 예약 불가능 상태로 구매 실패")
    void purchase_WithPessimicticLock_ConcertScheduleNotAvailable_ThrowsException() {
        // Given
        when(ticketRepository.findByIdWithLock(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        doThrow(new ReservationNotValidException(MessageCode.CONCERT_SCHEDULE_NOT_AVAILABLE))
                .when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);

        // When & Then
        assertThatThrownBy(() -> purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request))
                .isInstanceOf(ReservationNotValidException.class);

        verify(ticketDomainService).validateUserHasEnoughPoint(userId, useAmount);
        verify(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        verify(ticketRepository, never()).save(any());
        verify(ticketDomainService, never()).useUserPoint(any(), any());
    }

    @Test
    @DisplayName("티켓이 이미 예약된 상태로 구매 실패")
    void purchase_WithPessimicticLock_TicketAlreadyReserved_ThrowsException() {
        // Given
        when(ticketRepository.findByIdWithLock(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        doThrow(new ParameterNotValidException(MessageCode.TICKET_ALREADY_RESERVED_ERROR))
                .when(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);

        // When & Then
        assertThatThrownBy(() -> purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request))
                .isInstanceOf(ParameterNotValidException.class);

        verify(ticketDomainService).validateUserHasEnoughPoint(userId, useAmount);
        verify(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        verify(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        verify(ticketRepository, never()).save(any());
        verify(ticketDomainService, never()).useUserPoint(any(), any());
    }

    @Test
    @DisplayName("useUserPoint 실행 중 예외 발생으로 구매 실패")
    void purchase_WithPessimicticLock_UseUserPointFails_ThrowsException() {
        // Given
        when(ticketRepository.findByIdWithLock(ticketId)).thenReturn(Optional.of(mockTicket));
        when(ticketDomainService.validateUserHasEnoughPoint(userId, useAmount)).thenReturn(mockUser);
        doNothing().when(ticketDomainService).validateConcertScheduleAvailable(concertScheduleId);
        doNothing().when(ticketDomainService).validateTicketCanBeReserved(mockTicket, userId);
        doThrow(new RuntimeException("포인트 사용 실패"))
                .when(ticketDomainService).useUserPoint(mockUser, useAmount);

        // When & Then
        assertThatThrownBy(() -> purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("포인트 사용 실패");

        verify(ticketDomainService).useUserPoint(mockUser, useAmount);
        verify(ticketRepository, never()).save(any());
    }
}