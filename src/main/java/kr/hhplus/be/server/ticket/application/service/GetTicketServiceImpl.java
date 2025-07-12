package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.application.port.in.GetTicketUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.GetTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetTicketServiceImpl implements GetTicketUseCase {

    private final TicketRepositoryImpl ticketJpaRepository;

    @Override
    public GetTicketCommandDto.Response getTicket(Long ticketId) {
        Ticket ticket = ticketJpaRepository.findById(ticketId).orElseThrow(() -> new ParameterNotValidException(MessageCode.TICKET_NOT_FOUND));
        return GetTicketCommandDto.Response.fromDomain(ticket);
    }
}
