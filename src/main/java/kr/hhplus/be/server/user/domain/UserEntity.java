package kr.hhplus.be.server.user.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity {

    @Id
    @Tsid
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "point_amount")
    private BigDecimal pointAmount;

    public void chargePoint(BigDecimal point) {
        this.pointAmount = this.pointAmount.add(point);
    }

    public void usePoint(BigDecimal point) {
        this.pointAmount = this.pointAmount.subtract(point);
    }

    public boolean hasEnoughPoint(BigDecimal amount) {
        return this.pointAmount.compareTo(amount) >= 0;
    }

}
