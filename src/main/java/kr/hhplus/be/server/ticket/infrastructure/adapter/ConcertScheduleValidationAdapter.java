package kr.hhplus.be.server.ticket.infrastructure.adapter;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.repository.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.ticket.domain.port.ConcertScheduleValidationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertScheduleValidationAdapter implements ConcertScheduleValidationPort {

    private final ConcertScheduleJpaRepository scheduleRepository;

    @Override
    public boolean isAvailableForReservation(Long scheduleId) {
        ConcertScheduleEntity schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ParameterNotValidException(MessageCode.CONCERT_SCHEDULE_NOT_FOUND));

        return schedule.isAvailableForReservation();
    }

    @Override
    public void concertScheduleExist(Long concertScheduleId) {
        scheduleRepository.findById(concertScheduleId).orElseThrow(() -> new ParameterNotValidException(MessageCode.CONCERT_SCHEDULE_NOT_FOUND));
    }
}
