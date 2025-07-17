package kr.hhplus.be.server.ticket.application.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.application.port.in.ReserveTicketUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service("reserveTicketAtomicService")
@RequiredArgsConstructor
public class ReserveTicketAtomicServiceImpl implements ReserveTicketUseCase {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public ReserveTicketCommandDto.Response reserve(ReserveTicketCommandDto.Request request) {
        Long ticketId = request.getTicketId();
        Long userId = request.getUserId();

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5);
        int updatedRows = ticketRepository.reserveTicketAtomically(ticketId, userId, expireTime);

        if (updatedRows == 0) {
            throw new ParameterNotValidException(MessageCode.TICKET_ALREADY_RESERVED_ERROR);
        }

        return new ReserveTicketCommandDto.Response(ticketId);
    }
}
