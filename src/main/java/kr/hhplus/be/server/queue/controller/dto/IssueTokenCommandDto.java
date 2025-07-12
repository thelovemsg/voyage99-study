package kr.hhplus.be.server.queue.controller.dto;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IssueTokenCommandDto {

    @Getter
    @Setter
    public static class Request {
        private Long userId;
        private Long concertScheduleId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String token;
        private LocalDateTime expiresAt;
        private String message;
        private boolean success;
    }
}
