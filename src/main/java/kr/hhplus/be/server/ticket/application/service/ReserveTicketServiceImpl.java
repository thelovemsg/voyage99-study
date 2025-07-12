package kr.hhplus.be.server.ticket.application.service;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.application.port.in.ReserveTicketUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;

@Service("reserveTicketService")
@RequiredArgsConstructor
public class ReserveTicketServiceImpl implements ReserveTicketUseCase {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ReserveTicketCommandDto.Response reserve(ReserveTicketCommandDto.Request request) {
        Long ticketId = request.getTicketId();
        Long userId = request.getUserId();

        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() -> new NotFoundException(MessageCode.TICKET_NOT_FOUND, ticketId));

        ticket.reserve(userId);

        ticketRepository.save(ticket);

        return new ReserveTicketCommandDto.Response(ticketId);
    }

}