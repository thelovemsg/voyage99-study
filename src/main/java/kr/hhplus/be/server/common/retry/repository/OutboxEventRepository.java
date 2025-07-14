package kr.hhplus.be.server.common.retry.repository;

import kr.hhplus.be.server.common.retry.model.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByProcessedFalseOrderByCreatedAtAsc();

    @Query("SELECT o FROM OutboxEventEntity  o WHERE o.processed = false AND o.retryCount < o.maxRetry")
    List<OutboxEventEntity> findRetryableEvents();

    @Modifying
    @Query("DELETE FROM OutboxEventEntity o WHERE o.processed = true AND o.processedAt < :cutoffDate")
    void deleteProcessedEventsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
