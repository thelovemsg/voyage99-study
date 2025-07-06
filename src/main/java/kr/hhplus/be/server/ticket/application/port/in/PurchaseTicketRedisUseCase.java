package kr.hhplus.be.server.ticket.application.port.in;

import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;

public interface PurchaseTicketRedisUseCase {
    PurchaseTicketCommandDto.Response purchase(PurchaseTicketCommandDto.Request request);
}
