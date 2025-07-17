package kr.hhplus.be.server.ticket.domain.port;

public interface ConcertScheduleValidationPort {
    boolean isAvailableForReservation(Long scheduleId);
    void concertScheduleExist(Long concertScheduleId);
}
