package kr.hhplus.be.server.common.exceptions;

import kr.hhplus.be.server.common.messages.MessageCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(MessageCode code, Object... args) {
        super(code.format(args));
    }
}
