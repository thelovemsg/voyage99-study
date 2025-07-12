package kr.hhplus.be.server.concert.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "concert")
// WARNING!
// 새로운 객체를 생성하는 거기 때문에 toBuilder를 사용해도 영속성 관리가 되지 않아 dirty checking 일 발생하지 않는다.
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@DynamicUpdate
public class VenueEntity extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "venue_id")
    private Long venueId;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "venue_address")
    private String venueAddress;

    @Column(name = "venue_call_number")
    private String venueCallNumber;

    @Column(name = "total_capacity")
    private Integer totalCapacity;

    @Column(name = "description")
    private String description;

}
