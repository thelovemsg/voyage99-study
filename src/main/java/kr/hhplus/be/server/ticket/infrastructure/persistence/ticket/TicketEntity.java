package kr.hhplus.be.server.ticket.infrastructure.persistence.ticket;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.domain.SeatEntity;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.user.domain.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ticket")
@Getter
public class TicketEntity {

    @Id
    @Tsid
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "concert_schedule_id")
    private Long concertScheduleId;

    @Column(name = "ticket_no")
    private String ticketNo;

    @Column(name = "concert_info")
    private String concertInfo;

    @Column(name = "seat_info")
    private String seatInfo;

    @Column(name = "ticket_status")
    @Enumerated(EnumType.STRING)
    private TicketStatusEnum ticketStatusEnum;

    @Column(name = "purchase_datetime")
    private LocalDateTime purchaseDateTime;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // 예약 관련 필드 (임시 예약용)
    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    @Column(name = "reserved_by")
    private Long reservedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false
            , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", referencedColumnName = "seat_id", insertable = false, updatable = false
            , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private SeatEntity seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_schedule_id", referencedColumnName = "concert_schedule_id", insertable = false, updatable = false
            , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ConcertScheduleEntity concertSchedule;

    public void setTicketStatusEnum(TicketStatusEnum ticketStatusEnum) {
        this.ticketStatusEnum = ticketStatusEnum;
    }

    public void setReservedBy(Long reservedBy) {
        this.reservedBy = reservedBy;
    }

    public void setReservedUntil(LocalDateTime reservedUntil) {
        this.reservedUntil = reservedUntil;
    }
}
