package kr.hhplus.be.server.common.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.kafka.enums.KafkaTopics;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class KafkaEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.TICKET_PURCHASE_EVENTS, groupId = "concert-service-group")
    public void consumeTicketPurchaseEvent(
            @Payload String payload
    ) {
        try {
            ConcertTicketPurchaseEvent event = objectMapper.readValue(payload, ConcertTicketPurchaseEvent.class);

            // 비즈니스 로직 처리
            handleTicketPurchaseEvent(event);

            log.info("티켓 구매 이벤트 처리 완료: userId={}, ticketId={}",event.getUserId(), event.getTicketId());

        } catch (Exception e) {
            // 에러 처리 로직 (DLQ 전송 등)
        }
    }

    @KafkaListener(topics = KafkaTopics.CONCERT_SOLDOUT_EVENTS, groupId = "concert-service-group")
    public void consumeConcertSoldOutEvent(@Payload String payload) {  // ✅ key 파라미터 제거
        try {
            log.info("콘서트 매진 이벤트 수신");

            ConcertSoldOutEvent event = objectMapper.readValue(payload, ConcertSoldOutEvent.class);

            handleConcertSoldOutEvent(event);

            log.info("콘서트 매진 이벤트 처리 완료: concertId={}", event.concertInfo());

        } catch (Exception e) {
            log.error("콘서트 매진 이벤트 처리 실패: payload={}", payload, e);
        }
    }

    // private 메서드들은 그대로
    private void handleTicketPurchaseEvent(ConcertTicketPurchaseEvent event) {
        log.info("consumer handling ticket purchase info... : {}", event.toString());
    }

    private void handleConcertSoldOutEvent(ConcertSoldOutEvent event) {
        log.info("consumer handling concert sold out info... : {}", event.toString());
    }
}