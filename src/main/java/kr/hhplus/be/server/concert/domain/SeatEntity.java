package kr.hhplus.be.server.concert.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import kr.hhplus.be.server.concert.enums.SeatStatusEnum;

@Entity
@Table(name = "seat")
public class SeatEntity {

    @Id
    @Tsid
    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "venue_id", nullable = false, updatable = false)
    private Long venueId;

    @Column(name = "`rows`")
    private Integer rows;

    @Column(name = "`columns`")
    private Integer columns;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private SeatStatusEnum status = SeatStatusEnum.GOOD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", referencedColumnName = "venue_id", insertable = false, updatable = false
            , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private VenueEntity venue;

}
