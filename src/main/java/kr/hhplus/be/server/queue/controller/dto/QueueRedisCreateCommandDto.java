package kr.hhplus.be.server.queue.controller.dto;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueRedisCreateCommandDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
        private Long userId;
        private Long concertScheduleId;
        private String token;

        // 대기열 상세 정보
        private int position;           // 현재 순위
        private int peopleAhead;        // 앞에 있는 사람 수
        private int totalWaiting;       // 전체 대기 인원
        private int estimatedWaitMinutes; // 예상 대기 시간
        private int currentBatch;       // 현재 배치 번호
        private int positionInBatch;    // 배치 내 순서

        private String status;          // WAITING, PROCESSING 등
        private String message;
        private LocalDateTime createdAt;
    }
}
