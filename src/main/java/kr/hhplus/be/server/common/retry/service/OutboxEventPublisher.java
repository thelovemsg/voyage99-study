package kr.hhplus.be.server.common.retry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.retry.model.EventTypeEnum;
import kr.hhplus.be.server.common.retry.model.OutboxEventEntity;
import kr.hhplus.be.server.common.retry.repository.OutboxEventRepository;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("처리할 Outbox 이벤트 {}개 발견", pendingEvents.size());

        for (OutboxEventEntity outboxEvent : pendingEvents) {
            try {
                // Outbox에서 이벤트 복원
                Object event = deserializeEvent(outboxEvent);

                // ✅ 여기서 기존 이벤트 리스너들이 처리!
                eventPublisher.publishEvent(event);

                // 성공 시 처리 완료 표시
                outboxEvent.markAsProcessed();
                outboxEventRepository.save(outboxEvent);

                log.info("Outbox 이벤트 발행 성공: id={}, type={}",
                        outboxEvent.getId(), outboxEvent.getEventType());

            } catch (Exception e) {
                outboxEvent.incrementRetry();
                outboxEventRepository.save(outboxEvent);

                if (outboxEvent.canRetry()) {
                    log.warn("Outbox 이벤트 발행 실패, 재시도 예정: id={}, retryCount={}",
                            outboxEvent.getId(), outboxEvent.getRetryCount(), e);
                } else {
                    log.error("Outbox 이벤트 최대 재시도 초과: id={}, 수동 처리 필요",
                            outboxEvent.getId(), e);
                }
            }
        }
    }

    /**
     * 이벤트 타입에 따라 역직렬화
     */
    private Object deserializeEvent(OutboxEventEntity outboxEvent) throws Exception {
        EventTypeEnum eventType = outboxEvent.getEventType();
        String payload = outboxEvent.getPayload();

        return switch (eventType) {
            case TICKET_PURCHASED -> objectMapper.readValue(payload, ConcertTicketPurchaseEvent.class);
            case CONCERT_SOLD_OUT -> objectMapper.readValue(payload, ConcertSoldOutEvent.class);
        };
    }
}
