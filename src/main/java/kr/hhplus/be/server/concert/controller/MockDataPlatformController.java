package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.concert.controller.mock.SendTicketInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-flatform/mock")
@RequiredArgsConstructor
public class MockDataPlatformController {

    @PostMapping("/ticket-info")
    public ResponseEntity<String> sendTicketInfo(@RequestBody SendTicketInfoDto sendTicketInfoDto) {
        return null;
    }

}
