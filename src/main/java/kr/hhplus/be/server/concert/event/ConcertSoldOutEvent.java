package kr.hhplus.be.server.concert.event;

import java.time.LocalDateTime;

public record ConcertSoldOutEvent(Long concertScheduleId, String concertInfo, LocalDateTime soldOutDatetime) {
}
