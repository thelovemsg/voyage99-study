package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.ConcertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertJpaRepository extends JpaRepository<ConcertEntity, Long> {
    ConcertEntity save(ConcertEntity concert);
}
