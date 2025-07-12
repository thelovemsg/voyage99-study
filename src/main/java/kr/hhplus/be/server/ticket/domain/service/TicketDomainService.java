package kr.hhplus.be.server.ticket.domain.service;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.exceptions.ReservationNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.port.ConcertScheduleValidationPort;
import kr.hhplus.be.server.ticket.domain.port.UserValidationPort;
import kr.hhplus.be.server.user.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TicketDomainService {

    private final UserValidationPort userValidationPort;           // 포트 의존
    private final ConcertScheduleValidationPort scheduleValidationPort; // 포트 의존

    public void validateTicketCanBeReserved(Ticket ticket, Long userId) {
        if (ticket.isReservedByOther(userId) && !ticket.isReservationExpired() || !ticket.getUserId().equals(userId)) {
            throw new ParameterNotValidException(MessageCode.TICKET_ALREADY_RESERVED_ERROR);
        }
    }

    public UserEntity validateUserHasEnoughPoint(Long userId, BigDecimal useAmount) {
        return userValidationPort.validateAndGetUser(userId, useAmount);
    }

    public void validateConcertScheduleAvailable(Long scheduleId) {
        if (!scheduleValidationPort.isAvailableForReservation(scheduleId)) {
            throw new ReservationNotValidException(MessageCode.CONCERT_SCHEDULE_NOT_AVAILABLE);
        }

    }

    public void useUserPoint(UserEntity userEntity, BigDecimal amount) {
        userEntity.usePoint(amount);
        userValidationPort.saveUser(userEntity);
    }
}
