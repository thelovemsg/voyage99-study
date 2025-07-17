package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatJpaRepository extends JpaRepository<SeatEntity, Long> {
}
