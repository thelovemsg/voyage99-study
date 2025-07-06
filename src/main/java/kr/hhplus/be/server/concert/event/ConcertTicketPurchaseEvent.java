package kr.hhplus.be.server.concert.event;

import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcertTicketPurchaseEvent {

    private Long ticketId;
    private Long userId;
    private Long seatId;
    private Long concertScheduleId;
    private String ticketNo;
    private String concertInfo;
    private String seatInfo;
    private TicketStatusEnum ticketStatus;
    private LocalDateTime purchaseDateTime;
    private BigDecimal totalAmount;
    private LocalDateTime cancelledAt;

    public static ConcertTicketPurchaseEvent fromTicketEntity(Ticket ticket) {
        return ConcertTicketPurchaseEvent
                .builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUserId())
                .seatId(ticket.getSeatId())
                .concertScheduleId(ticket.getConcertScheduleId())
                .ticketNo(ticket.getTicketNo())
                .concertInfo(ticket.getConcertInfo())
                .seatInfo(ticket.getSeatInfo())
                .ticketStatus(ticket.getTicketStatus())
                .purchaseDateTime(ticket.getPurchaseDateTime())
                .totalAmount(ticket.getTotalAmount())
                .cancelledAt(ticket.getCancelledAt())
                .build();
    }
}
