package kr.hhplus.be.server.concert.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import kr.hhplus.be.server.concert.enums.CommonStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;

@Entity
@Table(name = "concert")
@Builder(toBuilder = true) // 새로운 객체를 생성하는 거기 때문에 toBuilder를 사용해도 영속성 관리가 되지 않아 dirty checking 일 발생하지 않는다.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@DynamicUpdate
public class ConcertEntity extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "concert_id")
    private Long concertId;
    
    @Column(name = "concert_name", length = 200)
    private String concertName;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(name = "description", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommonStatusEnum status = CommonStatusEnum.STOPPED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public boolean isConcertAvailable() {
        if(ObjectUtils.isEmpty(startDate) || ObjectUtils.isEmpty(endDate))
            return false;

        return this.status != CommonStatusEnum.STOPPED && this.status != CommonStatusEnum.READY;
    }

    public void openConcert() {
        this.status = CommonStatusEnum.ON_SELLING;
    }

    public void updateInfo(String concertName, String description, String artistName) {
        this.concertName = concertName;
        this.description = description;
        this.artistName = artistName;
    }

}
