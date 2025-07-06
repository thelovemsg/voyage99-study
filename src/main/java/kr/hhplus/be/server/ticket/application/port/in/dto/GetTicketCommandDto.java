package kr.hhplus.be.server.ticket.application.port.in.dto;

import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GetTicketCommandDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private Long ticketId;
        private Long concertScheduleId;
        private String ticketNo;
        private String concertInfo;
        private String seatInfo;
        private TicketStatusEnum ticketStatus;
        private LocalDateTime purchaseDateTime;
        private BigDecimal totalAmount;

        public static Response fromDomain(Ticket ticket) {
            return Response.builder()
                    .ticketId(ticket.getTicketId())
                    .concertScheduleId(ticket.getConcertScheduleId())
                    .ticketNo(ticket.getTicketNo())
                    .concertInfo(ticket.getConcertInfo())
                    .seatInfo(ticket.getSeatInfo())
                    .ticketStatus(ticket.getTicketStatus())
                    .purchaseDateTime(ticket.getPurchaseDateTime())
                    .totalAmount(ticket.getTotalAmount())
                    .build();
        }
    }
}
