package kr.hhplus.be.server.common.exceptions;

import kr.hhplus.be.server.common.messages.MessageCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TicketPurchaseException extends RuntimeException {

    public TicketPurchaseException(String message) {
        super(message);
    }

    public TicketPurchaseException(MessageCode code, Object... args) {
        super(code.format(args));
    }
}
