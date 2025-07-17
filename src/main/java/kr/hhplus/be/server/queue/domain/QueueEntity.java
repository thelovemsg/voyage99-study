package kr.hhplus.be.server.queue.domain;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import kr.hhplus.be.server.common.jpa.BaseEntity;
import kr.hhplus.be.server.queue.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "waiting_queue")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntity extends BaseEntity {

    @Id
    @Tsid
    @Column(name = "queue_id")
    private Long queueId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "concert_schedule_id", nullable = false)
    private Long concertScheduleId;

    @Column(name = "queue_position", nullable = true)
    @Builder.Default
    private Integer queuePosition = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "encrypted_token")
    private String encryptedToken;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    // 정적 팩토리 메서드 추가
    public static QueueEntity createQueue(Long userId, Long concertScheduleId, Integer position, String encryptedToken) {
        return QueueEntity.builder()
                .userId(userId)
                .concertScheduleId(concertScheduleId)
                .queuePosition(position)
                .status(QueueStatus.WAITING)
                .encryptedToken(encryptedToken)
                .build();
    }

    // 토큰 발급 메서드
    public void issueToken(String encryptedToken) {
        this.status = QueueStatus.ACTIVE;
        this.encryptedToken = encryptedToken;
        this.activatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    // 완료 처리
    public void complete() {
        this.status = QueueStatus.COMPLETED;
    }

    // 토큰 만료 처리
    public void expireToken() {
        this.status = QueueStatus.EXPIRED;
    }
    // 다음 순서인지 확인
    public boolean isNextInLine(QueueEntity nextQueue) {
        return nextQueue != null && this.queueId.equals(nextQueue.getQueueId());
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void activate() {
        this.status = QueueStatus.ACTIVE;
    }

    public void incrementRetryCount() {

    }
}
