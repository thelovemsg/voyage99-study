package kr.hhplus.be.server.ticket.application.port.in;

import kr.hhplus.be.server.ticket.application.port.in.dto.CancelTicketCommandDto;

public interface CancelTicketUseCase {
    CancelTicketCommandDto.Response complete(CancelTicketCommandDto.Request request);
}
