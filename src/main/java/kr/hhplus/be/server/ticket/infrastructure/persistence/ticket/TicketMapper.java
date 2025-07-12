package kr.hhplus.be.server.ticket.infrastructure.persistence.ticket;

import kr.hhplus.be.server.ticket.domain.model.Ticket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * JPA Entity ↔ Domain 변환은 기술적 구현 세부사항이기 때문에
 * infrastructure에 위치해야 한다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketMapper {

    public static TicketEntity toEntity(Ticket domain) {
        if (domain == null) return null;

        return TicketEntity.builder()
                .ticketId(domain.getTicketId())
                .userId(domain.getUserId())
                .seatId(domain.getSeatId())
                .concertScheduleId(domain.getConcertScheduleId())
                .ticketNo(domain.getTicketNo())
                .concertInfo(domain.getConcertInfo())
                .seatInfo(domain.getSeatInfo())
                .ticketStatusEnum(domain.getTicketStatus())
                .purchaseDateTime(domain.getPurchaseDateTime())
                .totalAmount(domain.getTotalAmount())
                .cancelledAt(domain.getCancelledAt())
                .reservedUntil(domain.getReservedUntil())
                .reservedBy(domain.getReservedBy())
                .build();
    }

    public static Ticket toDomain(TicketEntity entity) {
        if (entity == null) return null;

        return new Ticket(
                entity.getTicketId(),
                entity.getUserId(),
                entity.getSeatId(),
                entity.getConcertScheduleId(),
                entity.getTicketNo(),
                entity.getConcertInfo(),
                entity.getSeatInfo(),
                entity.getTicketStatusEnum(),
                entity.getPurchaseDateTime(),
                entity.getTotalAmount(),
                entity.getCancelledAt(),
                entity.getReservedUntil(),
                entity.getReservedBy()
        );
    }
}