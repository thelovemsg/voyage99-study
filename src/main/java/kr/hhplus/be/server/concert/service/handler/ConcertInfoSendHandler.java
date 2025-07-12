package kr.hhplus.be.server.concert.service.handler;


import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConcertInfoSendHandler {

    private final DataPlatformClient dataPlatformClient; // 외부 API 클라이언트

    @EventListener
    @Async
    public void handleConcertTicketPurchaseEvent(ConcertTicketPurchaseEvent event) {
        log.info("Concert ticket purchase event received :: {}", event);
        try {
            dataPlatformClient.sendTicketPurchaseInfo(event);
            log.info("DataPlatformClient 호출 완료");
        } catch (Exception e) {
            log.error("DataPlatformClient 호출 실패: {}", e.getMessage(), e);
        }
    }
}
