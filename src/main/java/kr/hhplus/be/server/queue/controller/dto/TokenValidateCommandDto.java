package kr.hhplus.be.server.queue.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.hhplus.be.server.queue.domain.QueueEntity;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenValidateCommandDto {
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long userId;
        private String token;
        private Long concertScheduleId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @JsonProperty("isValid")
        private boolean isValid;
        private String message;
        private LocalDateTime expiresAt;
        private String status;

    }
}
