package kr.hhplus.be.server.queue.repository;

import kr.hhplus.be.server.queue.domain.QueueEntity;
import kr.hhplus.be.server.queue.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<QueueEntity, Long> {
    
    //사용자 아이디로 조회
    Optional<QueueEntity> findByUserIdAndConcertScheduleId(Long userId, Long concertScheduleId);

    // 암호화된 토큰과 상태로 대기열 조회
    Optional<QueueEntity> findByEncryptedTokenAndStatus(String encryptedToken, QueueStatus status);

    // 사용자 ID와 상태로 대기열 조회
    Optional<QueueEntity> findByUserIdAndStatus(Long userId, QueueStatus status);

    Optional<QueueEntity> findByUserIdAndConcertScheduleIdAndStatus(Long userId, Long concertScheduleId, QueueStatus queueStatus);
    
    // 특정 상태와 콘서트 스케줄로 대기열 개수 조회
    int countByStatusAndConcertScheduleId(QueueStatus status, Long concertScheduleId);

    Optional<QueueEntity> findByUserIdAndConcertScheduleIdAndEncryptedToken(Long userId, Long concertScheduleId, String encryptedToken);

    Collection<QueueEntity> findByStatusOrderByQueuePositionAsc(QueueStatus queueStatus);
}
