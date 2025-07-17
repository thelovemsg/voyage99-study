package kr.hhplus.be.server.queue.controller.dto;

import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.queue.domain.QueueEntity;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueCreateCommandDto {

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
        private Long queueId;
        private Long userId;
        private String status;
        private int position;
        private String message;
        private String token;
        private LocalDateTime createdAt;

        public static Response fromEntity(QueueEntity queueEntity, String userToken, String message) {
            return Response.builder()
                    .queueId(queueEntity.getQueueId())
                    .userId(queueEntity.getUserId())
                    .token(userToken)
                    .status(queueEntity.getStatus().name())
                    .message(message)
                    .createdAt(queueEntity.getCreatedAt())
                    .build();
        }
    }
}
