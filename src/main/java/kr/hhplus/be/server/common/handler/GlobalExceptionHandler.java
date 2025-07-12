package kr.hhplus.be.server.common.handler;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParameterNotValidException.class)
    public ResponseEntity<?> handleParameterNotValid(ParameterNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)  // 400으로 설정
                .body(e.getMessage());
    }
}
