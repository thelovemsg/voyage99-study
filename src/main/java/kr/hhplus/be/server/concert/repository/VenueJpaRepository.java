package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueJpaRepository extends JpaRepository<VenueEntity, Long> {
}
