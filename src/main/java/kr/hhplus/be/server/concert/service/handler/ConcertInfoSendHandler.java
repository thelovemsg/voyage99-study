package kr.hhplus.be.server.concert.service.handler;


import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertInfoSendHandler {

    @EventListener
    @Async
    public void handleConcertTicketPurchaseEvent(ConcertTicketPurchaseEvent event) {
    }
}
