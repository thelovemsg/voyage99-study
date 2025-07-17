package kr.hhplus.be.server.queue.controller.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueDetailInfo {
    private Long userId;
    private Long concertScheduleId;
    private int currentPosition;
    private int peopleAhead;
    private int totalWaiting;
    private int estimatedWaitMinutes;
    private int currentBatch;
    private int positionInBatch;
}
