package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
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
@DisplayName("ReserveTicketServiceImpl 테스트")
class ReserveTicketServiceImplTest {

    @Mock
    private TicketRepositoryImpl ticketRepositoryImpl;

    @InjectMocks
    private ReserveTicketServiceImpl reserveTicketService;

    private Long ticketId;
    private Long userId1;
    private Long userId2;
    private Long seatId;
    private Long concertScheduleId;
    private Ticket ticket;

    @BeforeEach
    void setup() {
        this.ticketId = IdUtils.getNewId();
        this.userId1 = IdUtils.getNewId();
        this.userId2 = IdUtils.getNewId(); // 🆕 두 번째 사용자
        this.seatId = IdUtils.getNewId();
        this.concertScheduleId = IdUtils.getNewId();

        ticket = new Ticket(
                ticketId, null, seatId, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.AVAILABLE,
                null, new BigDecimal("10000"), null, null, null
        );
    }

    @Test
    @DisplayName("사용가능한 티켓 예약 성공 테스트")
    void reserve_available_ticket_success() {
        // given
        ReserveTicketCommandDto.Request request = createReserveRequest(ticketId, userId1);
        when(ticketRepositoryImpl.findByIdWithLock(ticketId)).thenReturn(Optional.of(ticket));

        // when
        ReserveTicketCommandDto.Response response = reserveTicketService.reserve(request);

        // then
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        assertThat(ticket.getTicketStatus()).isEqualTo(TicketStatusEnum.RESERVED);
        assertThat(ticket.getReservedBy()).isEqualTo(userId1);
        assertThat(ticket.getReservedUntil()).isAfter(LocalDateTime.now());

        // save 메서드가 호출되었는지 검증
        verify(ticketRepositoryImpl).save(any(Ticket.class));
    }

    @Test
    @DisplayName("이미 예약된 티켓을 다른 사람이 예약 시 실패 테스트")
    void reserve_already_reserved_ticket_by_other_user_fails() {
        // given - 첫 번째 사용자가 이미 예약한 상태
        Ticket reservedTicket = new Ticket(
                ticketId, null, seatId, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.RESERVED,
                null, new BigDecimal("10000"), null,
                LocalDateTime.now().plusMinutes(5), userId1
        );

        ReserveTicketCommandDto.Request request = createReserveRequest(ticketId, userId2);
        when(ticketRepositoryImpl.findByIdWithLock(ticketId)).thenReturn(Optional.of(reservedTicket));

        // when & then
        assertThatThrownBy(() -> reserveTicketService.reserve(request))
                .isInstanceOf(ParameterNotValidException.class)
                .hasMessageContaining(MessageCode.TICKET_RESERVATION_NOT_AVAILABLE.getMessage());

        // 상태가 변경되지 않았는지 확인
        assertThat(reservedTicket.getReservedBy()).isEqualTo(userId1); // 여전히 첫 번째 사용자

        // save가 호출되지 않았는지 확인 (예외 발생으로 인해)
        verify(ticketRepositoryImpl, never()).save(any(Ticket.class));
    }
    @Test
    @DisplayName("만료된 예약을 다른 사용자가 재예약 성공 테스트")
    void reserve_expired_reservation_by_other_user_success() {
        // given - 첫 번째 사용자의 예약이 만료된 상태로 직접 생성
        Ticket expiredReservedTicket = new Ticket(
                ticketId, null, seatId, concertScheduleId, "TKT001",
                "콘서트 정보", "좌석 정보", TicketStatusEnum.RESERVED,
                null, new BigDecimal("10000"), null,
                LocalDateTime.now().minusMinutes(1), userId1  // 🆕 1분 전으로 설정 = 만료됨
        );

        ReserveTicketCommandDto.Request request = createReserveRequest(ticketId, userId2);
        when(ticketRepositoryImpl.findByIdWithLock(ticketId)).thenReturn(Optional.of(expiredReservedTicket));

        // when
        ReserveTicketCommandDto.Response response = reserveTicketService.reserve(request);

        // then
        assertThat(response.getTicketId()).isEqualTo(ticketId);
        assertThat(expiredReservedTicket.getTicketStatus()).isEqualTo(TicketStatusEnum.RESERVED);
        assertThat(expiredReservedTicket.getReservedBy()).isEqualTo(userId2); // 새로운 사용자로 변경됨
        assertThat(expiredReservedTicket.getReservedUntil()).isAfter(LocalDateTime.now()); // 새로운 만료 시간

        verify(ticketRepositoryImpl).save(any(Ticket.class));
    }

    private ReserveTicketCommandDto.Request createReserveRequest(Long ticketId, Long userId) {
        ReserveTicketCommandDto.Request request = new ReserveTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        return request;
    }
}