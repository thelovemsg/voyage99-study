package kr.hhplus.be.server.ticket.application.port.in.dto;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PurchaseTicketCommandDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request{
        private Long userId;
        private Long ticketId;
        private Long seatId;
        private Long concertScheduleId;
        private BigDecimal useAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long ticketId;
        private boolean isSuccess;
    }
}
