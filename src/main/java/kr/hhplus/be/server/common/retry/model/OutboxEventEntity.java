package kr.hhplus.be.server.common.retry.model;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_outbox_event")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEventEntity extends BaseEntity {

    @Id
    @Tsid
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventTypeEnum eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = Boolean.FALSE;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retry")
    @Builder.Default
    private Integer maxRetry = 3;

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetry;
    }
}
