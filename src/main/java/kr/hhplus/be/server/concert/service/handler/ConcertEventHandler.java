package kr.hhplus.be.server.concert.service.handler;

import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.service.ConcertScheduleService;
import kr.hhplus.be.server.concert.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertEventHandler {

    private final ConcertScheduleService concertScheduleService;
    private final RankingService rankingService;

    @EventListener
    @Async
    public void handleConcertSoldOut(ConcertSoldOutEvent event) {
        concertScheduleService.markAsSoldOut(event.concertScheduleId());
        rankingService.addToRanking(event.concertScheduleId(), event.concertInfo(), event.soldOutDatetime());
    }
}
