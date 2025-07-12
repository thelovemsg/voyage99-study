package kr.hhplus.be.server.concert.controller.dto;

import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertScheduleCreateDto {

    @Getter
    @Setter
    public static class Request {
        private Long concertId;
        private Long venueId;
        private LocalDate concertDate;
        private LocalTime concertStartTime;
        private LocalTime concertEndTime;
        private Integer totalSeatsNumber;
        private CommonStatusEnum scheduleStatus;

        public static ConcertScheduleEntity toEntity(Request request) {
            return ConcertScheduleEntity
                    .builder()
                    .concertId(request.getConcertId())
                    .venueId(request.getVenueId())
                    .concertDate(request.getConcertDate())
                    .concertStartTime(request.getConcertStartTime())
                    .concertEndTime(request.getConcertEndTime())
                    .totalSeatsNumber(request.getTotalSeatsNumber())
                    .scheduleStatus(request.getScheduleStatus())
                    .build();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Long concertScheduleId;

        public static Response fromEntity(ConcertScheduleEntity savedSchedule) {
            return new ConcertScheduleCreateDto.Response(savedSchedule.getConcertScheduleId());
        }
    }
}
