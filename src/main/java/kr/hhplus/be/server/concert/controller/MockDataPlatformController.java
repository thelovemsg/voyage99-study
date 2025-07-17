package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/data-platform/mock")
@RequiredArgsConstructor
public class MockDataPlatformController {

    @PostMapping("/ticket-info")
    public ResponseEntity<String> sendTicketInfo(@RequestBody ConcertTicketPurchaseEvent event) {
        log.info("Send Ticket Info :: {}", event);
        return ResponseEntity.status(HttpStatus.OK).body(event.toString());
    }

}
