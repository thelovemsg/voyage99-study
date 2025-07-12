package kr.hhplus.be.server.queue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.hhplus.be.server.queue.controller.dto.QueueDetailInfo;
import kr.hhplus.be.server.queue.controller.dto.QueueRedisCreateCommandDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class QueueServiceWaitingQueueTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final Long TEST_CONCERT_SCHEDULE_ID = 999L;
    private final int TOTAL_USERS = 30;
    private final int BATCH_SIZE = 10;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 정리
        String queueKey = "queue:" + TEST_CONCERT_SCHEDULE_ID;
        String tokenPattern = "token:" + TEST_CONCERT_SCHEDULE_ID + ":*";

        redisTemplate.delete(queueKey);
        redisTemplate.delete(redisTemplate.keys(tokenPattern));

        log.info("=== 테스트 시작 - Redis 초기화 완료 ===");
    }

    @Test
    void 대기열_전체_처리_플로우_테스트() throws InterruptedException, JsonProcessingException {
        // Given: 30명의 사용자를 대기열에 추가
        log.info("\n 1단계: 30명 사용자 대기열 추가");
        add30UsersToQueue();

        // 초기 상태 확인
        verifyInitialQueueState();

        // When & Then: 배치 처리로 모든 사용자 처리
        int batchCount = 1;
        int remainingUsers = TOTAL_USERS;

        while (remainingUsers > 0) {
            log.info("\n {}번째 배치 처리 시작 (예상 처리: {}명)", batchCount, Math.min(BATCH_SIZE, remainingUsers));

            // 배치 처리 전 상태 확인
            logQueueStateBefore(batchCount);

            // 배치 처리 실행
            List<QueueDetailInfo> result = queueService.processTopUsers(TEST_CONCERT_SCHEDULE_ID, BATCH_SIZE);

            // 배치 처리 후 상태 확인
            logQueueStateAfter(batchCount, result);

            remainingUsers -= result.size();
            batchCount++;

            // 잠시 대기 (로그 확인을 위해)
            Thread.sleep(1000);
        }

        // 최종 상태 검증
        verifyFinalState();

        log.info("\n 모든 사용자 처리 완료! 총 {}번의 배치 처리", batchCount - 1);
    }

    private void add30UsersToQueue() throws JsonProcessingException {
        for (int i = 1; i <= TOTAL_USERS; i++) {
            Long userId = (long) i;

            QueueRedisCreateCommandDto.Request request = QueueRedisCreateCommandDto.Request.builder()
                    .userId(userId)
                    .concertScheduleId(TEST_CONCERT_SCHEDULE_ID)
                    .build();

            QueueRedisCreateCommandDto.Response response = queueService.enterQueueRedis(request);

            // 각 사용자의 진입 결과 검증
            assertEquals(userId, response.getUserId());
            assertEquals(i, response.getPosition()); // 1번째, 2번째, 3번째...
            assertEquals(i - 1, response.getPeopleAhead()); // 앞에 0명, 1명, 2명...

            if (i % 10 == 0) {
                log.info("{}명 추가 완료", i);
            }
        }

        log.info("✅ 총 {}명 대기열 추가 완료", TOTAL_USERS);
    }

    private void verifyInitialQueueState() throws JsonProcessingException {
        String queueKey = "queue:" + TEST_CONCERT_SCHEDULE_ID;
        Long totalCount = redisTemplate.opsForZSet().zCard(queueKey);

        assertEquals(TOTAL_USERS, totalCount);
        log.info(" 초기 대기열 크기 검증 완료: {}명", totalCount);

        // 상위 10명 확인
        List<QueueDetailInfo> top10 = queueService.getCurrentProcessingUsers(TEST_CONCERT_SCHEDULE_ID);
        assertEquals(BATCH_SIZE, top10.size());
        assertEquals(Long.valueOf(1), top10.get(0).getUserId()); // 첫 번째는 user 1
        assertEquals(Long.valueOf(10), top10.get(9).getUserId()); // 10번째는 user 10

        log.info(" 상위 10명 검증 완료: {} ~ {}", top10.get(0).getUserId(), top10.get(9).getUserId());
    }

    private void logQueueStateBefore(int batchNumber) throws JsonProcessingException {
        String queueKey = "queue:" + TEST_CONCERT_SCHEDULE_ID;
        Long totalCount = redisTemplate.opsForZSet().zCard(queueKey);

        List<QueueDetailInfo> currentTop10 = queueService.getCurrentProcessingUsers(TEST_CONCERT_SCHEDULE_ID);
        List<Long> userIds = currentTop10.stream().map(QueueDetailInfo::getUserId).toList();

        log.info(" {}번째 배치 처리 전:", batchNumber);
        log.info("   - 전체 대기자: {}명", totalCount);
        log.info("   - 처리 예정 상위 10명: {}", userIds);
    }

    private void logQueueStateAfter(int batchNumber, List<QueueDetailInfo> result) throws JsonProcessingException {
        String queueKey = "queue:" + TEST_CONCERT_SCHEDULE_ID;
        Long remainingCount = redisTemplate.opsForZSet().zCard(queueKey);

        if(remainingCount == null) remainingCount = 0L;

        List<Long> processedUserIds = result.stream().map(QueueDetailInfo::getUserId).toList();

        log.info(" {}번째 배치 처리 후:", batchNumber);
        log.info("   - 처리된 사용자: {}명", result.size());
        log.info("   - 남은 대기자: {}명", remainingCount);
        log.info("   - 처리된 사용자 목록: {}", processedUserIds);

        // 남은 사용자가 있다면 다음 상위 10명도 표시
        if (remainingCount > 0) {
            List<QueueDetailInfo> nextTop10 = queueService.getCurrentProcessingUsers(TEST_CONCERT_SCHEDULE_ID);
            List<Long> nextUserIds = nextTop10.stream().map(QueueDetailInfo::getUserId).toList();
            log.info("   - 다음 처리 예정: {}", nextUserIds);
        }
    }

    private void verifyFinalState() {
        String queueKey = "queue:" + TEST_CONCERT_SCHEDULE_ID;
        Long finalCount = redisTemplate.opsForZSet().zCard(queueKey);

        assertEquals(0L, finalCount);
        log.info("최종 상태 검증 완료: 대기열이 완전히 비어있음");

        // 토큰들도 모두 제거되었는지 확인
        String tokenPattern = "token:" + TEST_CONCERT_SCHEDULE_ID + ":*";
        int tokenCount = redisTemplate.keys(tokenPattern).size();
        assertEquals(0L, tokenCount);
        log.info("모든 토큰 제거 확인 완료");
    }

}