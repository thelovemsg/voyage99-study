package kr.hhplus.be.server.ticket.application.port.in;

import kr.hhplus.be.server.ticket.application.port.in.dto.CreateTicketCommandDto;

public interface CreateTicketUseCase {
    CreateTicketCommandDto.Response createTicket(CreateTicketCommandDto.Request request);
}
