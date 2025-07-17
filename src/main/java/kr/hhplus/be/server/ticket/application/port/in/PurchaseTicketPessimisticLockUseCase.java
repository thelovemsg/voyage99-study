package kr.hhplus.be.server.ticket.application.port.in;

import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;

public interface PurchaseTicketPessimisticLockUseCase {
    PurchaseTicketCommandDto.Response purchaseWithPessimisticLock(PurchaseTicketCommandDto.Request request);
}
