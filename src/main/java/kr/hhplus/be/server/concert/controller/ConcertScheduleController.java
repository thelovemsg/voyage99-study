package kr.hhplus.be.server.concert.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.hhplus.be.server.concert.controller.dto.ConcertScheduleCreateDto;
import kr.hhplus.be.server.concert.controller.dto.ConcertScheduleInfoDto;
import kr.hhplus.be.server.concert.controller.handler.RankingInfo;
import kr.hhplus.be.server.concert.service.ConcertScheduleService;
import kr.hhplus.be.server.concert.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/concert/schedule")
@RequiredArgsConstructor
public class ConcertScheduleController {

    private final ConcertScheduleService concertScheduleService;
    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<ConcertScheduleInfoDto.Response> getConcertScheduleInfo(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(concertScheduleService.getConcertScheduleInfo(id));
    }

    @PostMapping
    public ResponseEntity<ConcertScheduleCreateDto.Response> createConcertSchedule(@RequestBody ConcertScheduleCreateDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(concertScheduleService.createConcertSchedule(request));
    }

    @GetMapping("/ranks")
    public ResponseEntity<List<RankingInfo>> getRankingInfoList() throws JsonProcessingException {
        return ResponseEntity.status(HttpStatus.OK).body(rankingService.getTodayRanking(10));
    }
}
