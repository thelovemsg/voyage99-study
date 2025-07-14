package kr.hhplus.be.server.concert.controller.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RankingData {
    private Long concertScheduleId;
    private String concertTitle;
    private LocalDateTime soldOutTime;

    // 개선 제안
    private Integer totalSeats;
    private Integer soldSeats;
    private Duration sellDuration;
    private String venueName;

    @JsonCreator
    public RankingData(
            @JsonProperty("concertScheduleId") Long concertScheduleId,
            @JsonProperty("concertTitle") String concertTitle,
            @JsonProperty("soldOutTime") LocalDateTime soldOutTime
    ) {
        this.concertScheduleId = concertScheduleId;
        this.concertTitle = concertTitle;
        this.soldOutTime = soldOutTime;
    }

}
