package kr.hhplus.be.server.ticket.application.port.in;

import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;

public interface ReserveTicketUseCase {
    ReserveTicketCommandDto.Response reserve(ReserveTicketCommandDto.Request request);
}
