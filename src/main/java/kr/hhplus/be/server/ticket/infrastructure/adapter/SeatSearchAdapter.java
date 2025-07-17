package kr.hhplus.be.server.ticket.infrastructure.adapter;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.concert.repository.SeatJpaRepository;
import kr.hhplus.be.server.ticket.domain.port.SeatSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatSearchAdapter implements SeatSearchPort {

    private final SeatJpaRepository seatJpaRepository;

    @Override
    public void seatExist(Long seatId) {
        seatJpaRepository.findById(seatId).orElseThrow(() -> new ParameterNotValidException(MessageCode.SEAT_NOT_FOUND));
    }
}
