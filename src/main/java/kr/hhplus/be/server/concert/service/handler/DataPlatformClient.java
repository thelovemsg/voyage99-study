package kr.hhplus.be.server.concert.service.handler;

import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformClient {

    private final WebClient webClient;

    public void sendTicketPurchaseInfo(ConcertTicketPurchaseEvent event) {
        webClient.post()
                .uri("/data-platform/mock/ticket-info")
                .bodyValue(event)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSubscribe(subscription -> log.info("HTTP 요청 구독 시작"))
                .doOnSuccess(response -> log.info("데이터 전송 성공: {}", response))
                .doOnError(error -> log.error("데이터 전송 실패: {}", error.getMessage()))
                .onErrorComplete() // 에러 무시하고 계속 진행
                .subscribe(); // 비동기 실행
    }

}
