package kr.hhplus.be.server.common.exceptions;

import kr.hhplus.be.server.common.messages.MessageCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ReservationNotValidException extends RuntimeException {
    public ReservationNotValidException(String message) {
        super(message);
    }

    public ReservationNotValidException(MessageCode code, Object... args) {
        super(code.format(args));
    }
}
