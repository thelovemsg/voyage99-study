package kr.hhplus.be.server.concert.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "concert_schedule")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ConcertScheduleEntity extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "concert_schedule_id")
    private Long concertScheduleId;

    @Column(name = "concert_id", nullable = false, updatable = false)
    private Long concertId;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private Long venueId;

    @Column(name = "concert_date")
    private LocalDate concertDate;

    @Column(name = "concert_start_time")
    private LocalTime concertStartTime;

    @Column(name = "concert_end_time")
    private LocalTime concertEndTime;

    @Column(name = "total_seats_number")
    private Integer totalSeatsNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status")
    private CommonStatusEnum scheduleStatus;

    @Column(name = "sold_out_dttm")
    private LocalDateTime soldOutDatetime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", referencedColumnName = "concert_id", insertable = false, updatable = false
                    , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ConcertEntity concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", referencedColumnName = "venue_id", insertable = false, updatable = false
            , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private VenueEntity venue;

    public boolean isAvailableForReservation() {
        return this.scheduleStatus == CommonStatusEnum.ON_SELLING;
    }

    public void soldOutTicket() {
        this.scheduleStatus = CommonStatusEnum.SOLD_OUT;
        this.soldOutDatetime = LocalDateTime.now();
    }
}
