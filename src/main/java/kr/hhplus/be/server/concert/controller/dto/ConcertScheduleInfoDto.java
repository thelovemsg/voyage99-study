package kr.hhplus.be.server.concert.controller.dto;

import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertScheduleInfoDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private Long concertScheduleId;
        private Long concertId;
        private Long venueId;
        private LocalDate concertDate;
        private LocalTime concertStartTime;
        private LocalTime concertEndTime;
        private Integer totalSeatsNumber;
        private CommonStatusEnum scheduleStatus;

        public static Response fromEntity(ConcertScheduleEntity entity) {
            return Response.builder()
                    .concertScheduleId(entity.getConcertScheduleId())
                    .concertId(entity.getConcertId())
                    .venueId(entity.getVenueId())
                    .concertDate(entity.getConcertDate())
                    .concertStartTime(entity.getConcertStartTime())
                    .concertEndTime(entity.getConcertEndTime())
                    .totalSeatsNumber(entity.getTotalSeatsNumber())
                    .scheduleStatus(entity.getScheduleStatus())
                    .build();
        }
    }
}
