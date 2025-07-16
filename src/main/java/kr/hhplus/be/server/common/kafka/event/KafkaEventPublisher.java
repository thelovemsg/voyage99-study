package kr.hhplus.be.server.common.kafka.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.kafka.enums.KafkaTopics;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishEvent(Object event) {
        try {
            String topicName = getTopicName(event);
            String eventKey = getEventKey(event);
            String eventPayload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topicName, eventKey, eventPayload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 메시지 전송 실패: topic={}, key={}", topicName, eventKey, ex);
                            throw new RuntimeException("Kafka 전송 실패", ex);
                        } else {
                            log.info("Kafka 메시지 전송 성공: topic={}, key={}", topicName, eventKey);
                        }
                    });

        } catch (Exception e) {
            log.error("이벤트 직렬화 실패: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }

    private String getTopicName(Object event) {
        if (event instanceof ConcertTicketPurchaseEvent) {
            return KafkaTopics.TICKET_PURCHASE_EVENTS;
        } else if (event instanceof ConcertSoldOutEvent) {
            return KafkaTopics.CONCERT_SOLDOUT_EVENTS;
        }
        throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + event.getClass().getSimpleName());
    }

    private String getEventKey(Object event) {
        if (event instanceof ConcertTicketPurchaseEvent ticketEvent) {
            return "ticket-" + ticketEvent.getTicketId();
        } else if (event instanceof ConcertSoldOutEvent soldOutEvent) {
            return "concert-" + soldOutEvent.concertInfo();
        }
        return "default-key";
    }
}