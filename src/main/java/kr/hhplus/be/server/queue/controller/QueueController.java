package kr.hhplus.be.server.queue.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.hhplus.be.server.queue.controller.dto.*;
import kr.hhplus.be.server.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue")
public class QueueController {
    
    private final QueueService queueService;
    
    /**
     * 큐 생성
     */
    @PostMapping("/enter")
    public ResponseEntity<QueueCreateCommandDto.Response> enterQueue(@RequestBody QueueCreateCommandDto.Request request) {
        QueueCreateCommandDto.Response response = queueService.enterQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 큐 생성 - redis
     */
    @PostMapping("/enter-redis")
    public ResponseEntity<QueueRedisCreateCommandDto.Response> enterQueueRedis(@RequestBody QueueRedisCreateCommandDto.Request request) throws JsonProcessingException {
        QueueRedisCreateCommandDto.Response response = queueService.enterQueueRedis(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 큐 정보 조회
     */
    @GetMapping("/{concertScheduleId}/top10")
    public ResponseEntity<List<QueueDetailInfo>> top10(@PathVariable Long concertScheduleId) throws JsonProcessingException {
        List<QueueDetailInfo> infos = queueService.getCurrentProcessingUsers(concertScheduleId);
        return ResponseEntity.status(HttpStatus.OK).body(infos);
    }

    /**
     * 큐 처리
     */
    @GetMapping("/{concertScheduleId}/process-queue")
    public ResponseEntity<List<QueueDetailInfo>> processQueue(@PathVariable Long concertScheduleId) throws JsonProcessingException {
        List<QueueDetailInfo> infoList = queueService.processTopUsers(concertScheduleId, 10);
        return ResponseEntity.status(HttpStatus.OK).body(infoList);
    }

    /**
     * 토큰 검증
     */
    @PostMapping("/token/validate")
    public ResponseEntity<TokenValidateCommandDto.Response> validateToken(@RequestBody TokenValidateCommandDto.Request request) {
        log.info("토큰 검증 요청: userId={}, concertScheduleId={}", 
                request.getUserId(), request.getConcertScheduleId());
        
        TokenValidateCommandDto.Response response = queueService.validateToken(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 큐 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<String> getQueueStatus(
            @RequestParam Long userId, 
            @RequestParam Long concertScheduleId) {
        log.info("큐 상태 확인 요청: userId={}, concertScheduleId={}", userId, concertScheduleId);
        
        // 기본적인 상태 확인 (추후 구현 가능)
        return ResponseEntity.ok("큐 상태 조회 기능은 추후 구현 예정입니다.");
    }



}
