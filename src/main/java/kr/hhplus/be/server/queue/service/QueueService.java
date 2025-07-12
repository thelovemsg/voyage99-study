package kr.hhplus.be.server.queue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.utils.CryptoUtils;
import kr.hhplus.be.server.queue.controller.dto.QueueCreateCommandDto;
import kr.hhplus.be.server.queue.controller.dto.QueueDetailInfo;
import kr.hhplus.be.server.queue.controller.dto.QueueRedisCreateCommandDto;
import kr.hhplus.be.server.queue.controller.dto.TokenValidateCommandDto;
import kr.hhplus.be.server.queue.domain.QueueEntity;
import kr.hhplus.be.server.queue.enums.QueueStatus;
import kr.hhplus.be.server.queue.enums.TokenStatus;
import kr.hhplus.be.server.queue.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;
    private final CryptoUtils cryptoUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisCacheTemplate redisCacheTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 큐 생성
     */
    @Transactional
    public QueueCreateCommandDto.Response enterQueue(QueueCreateCommandDto.Request request) {
        Long userId = request.getUserId();
        Long concertScheduleId = request.getConcertScheduleId();
        String userToken = null;

        Optional<QueueEntity> existingQueue = queueRepository.findByUserIdAndConcertScheduleId(userId, concertScheduleId);

        if (existingQueue.isPresent()) {
            QueueEntity queue = existingQueue.get();
            String message = MessageCode.QUEUE_ALREADY_EXISTS.getMessage();
            return QueueCreateCommandDto.Response.fromEntity(queue, userToken, message);
        }

        // 현재 대기열 위치 계산
        int currentPosition = queueRepository.countByStatusAndConcertScheduleId(QueueStatus.WAITING, concertScheduleId) + 1;

        userToken  = cryptoUtils.generateSecureToken();
        String encryptedToken = cryptoUtils.encrypt(userToken);

        QueueEntity queue = QueueEntity.createQueue(userId, concertScheduleId, currentPosition, encryptedToken);

        QueueEntity savedQueue = queueRepository.save(queue);

        log.info("사용자 {}의 큐가 생성되었습니다. Queue ID: {} / Concert schedule ID : {}", userId, savedQueue.getUserId(), concertScheduleId);

        String resultMessage = MessageCode.QUEUE_CREATE_SUCCESS.getMessage();
        return QueueCreateCommandDto.Response.fromEntity(queue, userToken, resultMessage);
    }

    /**
     * Redis 기반 대기열 진입 (대기 정보 포함)
     */
    public QueueRedisCreateCommandDto.Response enterQueueRedis(QueueRedisCreateCommandDto.Request request) throws JsonProcessingException {
        Long userId = request.getUserId();
        Long concertScheduleId = request.getConcertScheduleId();

        String queueKey = "queue:" + concertScheduleId;

        // 이미 대기열에 있는지 확인 (JSON으로 저장된 상태에서)
        Set<String> allUsers = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        for (String userJson : allUsers) {
            QueueDetailInfo existing = objectMapper.readValue(userJson, QueueDetailInfo.class);
            if (existing.getUserId().equals(userId)) {
                // 기존 사용자 발견
                return QueueRedisCreateCommandDto.Response.builder()
                        .userId(userId)
                        .position(existing.getCurrentPosition())
                        .message("이미 대기열에 있습니다.")
                        .build();
            }
        }

        // 먼저 현재 대기열 크기 확인
        Long currentQueueSize = redisTemplate.opsForZSet().zCard(queueKey);
        int newPosition = (currentQueueSize != null ? currentQueueSize.intValue() : 0) + 1;

        // 새로 대기열 진입
        long timestamp = System.currentTimeMillis();

        // QueueDetailInfo 객체를 JSON으로 저장
        QueueDetailInfo queueInfo = QueueDetailInfo.builder()
                .userId(userId)
                .concertScheduleId(concertScheduleId)
                .currentPosition(newPosition)
                .peopleAhead(newPosition - 1)
                .totalWaiting(newPosition)
                .build();

        // Redis에 대기열 추가
        redisCacheTemplate.addToSortedSet(queueKey, queueInfo, timestamp);

        // 미리 계산한 값으로 응답 생성
        int peopleAhead = newPosition - 1;
        int batchSize = 10;
        int currentBatch = (peopleAhead / batchSize) + 1;
        int positionInBatch = (peopleAhead % batchSize) + 1;
        int estimatedWaitMinutes = calculateEstimatedWaitTime(peopleAhead);

        log.info("Redis 대기열 진입 - 사용자: {}, 공연: {}, 순위: {}, 예상대기시간: {}분",
                userId, concertScheduleId, newPosition, estimatedWaitMinutes);

        return QueueRedisCreateCommandDto.Response.builder()
                .userId(userId)
                .concertScheduleId(concertScheduleId)
                .position(newPosition)
                .peopleAhead(peopleAhead)
                .totalWaiting(newPosition) // 새로 추가된 후의 전체 크기
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .currentBatch(currentBatch)
                .positionInBatch(positionInBatch)
                .message("대기열 진입 성공")
                .build();
    }

    private QueueDetailInfo calculateQueueInfo(Long userId, Long concertScheduleId) {
        String queueKey = "queue:" + concertScheduleId;

        // 현재 사용자의 순위
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());

        if (rank == null) {
            return QueueDetailInfo.builder()
                    .currentPosition(0)
                    .peopleAhead(0)
                    .totalWaiting(0)
                    .estimatedWaitMinutes(0)
                    .currentBatch(0)
                    .positionInBatch(0)
                    .build();
        }

        // 전체 대기 인원
        Long totalWaiting = redisTemplate.opsForZSet().zCard(queueKey);

        int peopleAhead = rank.intValue();
        int currentPosition = peopleAhead + 1;

        // 배치 정보 계산
        int batchSize = 10;
        int currentBatch = (peopleAhead / batchSize) + 1;
        int positionInBatch = (peopleAhead % batchSize) + 1;

        // 예상 대기 시간 계산
        int estimatedWaitMinutes = calculateEstimatedWaitTime(peopleAhead);

        return QueueDetailInfo.builder()
                .currentPosition(currentPosition)
                .peopleAhead(peopleAhead)
                .totalWaiting(totalWaiting.intValue())
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .currentBatch(currentBatch)
                .positionInBatch(positionInBatch)
                .build();
    }

    private int calculateEstimatedWaitTime(int peopleAhead) {
        int batchSize = 10; // 한 번에 10명 처리
        int batchInterval = 60; // 1분마다 배치 처리
        int processingTime = 300; // 구매 완료까지 5분 소요

        // 내가 속한 배치까지 몇 번의 배치가 필요한지
        int batchesUntilMyTurn = (peopleAhead / batchSize);

        // 배치 처리 대기 시간 + 구매 처리 시간
        int waitForBatch = batchesUntilMyTurn * batchInterval;
        int totalEstimatedTime = waitForBatch + processingTime;

        return totalEstimatedTime / 60; // 분 단위로 반환
    }

    /**
     * 맨 앞 N명을 처리 (대기열에서 제거)
     */
    public List<QueueDetailInfo> processTopUsers(Long concertScheduleId, int count) throws JsonProcessingException {
        String queueKey = "queue:" + concertScheduleId;

        // 상위 count명 가져오기 (0부터 count-1까지)
        Set<String> topUsers = redisTemplate.opsForZSet().range(queueKey, 0, count - 1);

        if (topUsers == null || topUsers.isEmpty()) {
            log.info("공연 {}의 대기열이 비어있습니다.", concertScheduleId);
            return new ArrayList<>();
        }

        List<QueueDetailInfo> result = new ArrayList<>();

        // 처리된 사용자들을 대기열에서 제거
        for (String userInfo : topUsers) {
            QueueDetailInfo detailInfo = objectMapper.readValue(userInfo, QueueDetailInfo.class);  // ← RankingData로 읽기
            result.add(detailInfo);
            removeUserFromQueue(detailInfo.getUserId(), concertScheduleId, userInfo);
        }

        log.info("공연 {} - {}명이 처리되어 대기열에서 제거되었습니다.", concertScheduleId, result.size());

        return result;
    }

    /**
     * 특정 사용자를 대기열에서 제거 (구매 완료 또는 시간 초과)
     */
    public void removeUserFromQueue(Long userId, Long concertScheduleId, String userInfo) {
        String queueKey = "queue:" + concertScheduleId;

        // 대기열에서 제거
        Long removed = redisTemplate.opsForZSet().remove(queueKey, userInfo);

        if (removed != null && removed > 0) {
            log.info("사용자 {}가 대기열에서 제거되었습니다. queue : {})", userId, queueKey);
        } else {
            log.warn("사용자 {}가 대기열에 없어서 제거할 수 없습니다.", userId);
        }
    }

    /**
     * 현재 처리 가능한 사용자들 조회 (상위 10명)
     */
    public List<QueueDetailInfo> getCurrentProcessingUsers(Long concertScheduleId) throws JsonProcessingException {
        String queueKey = "queue:" + concertScheduleId;

        Set<String> range = redisTemplate.opsForZSet().range(queueKey, 0, 9);
        Set<String> topUsers = range == null ? new HashSet<>() : range; // 상위 10명

        List<QueueDetailInfo> result = new ArrayList<>();

        for (String topUser : topUsers) {
            QueueDetailInfo detailInfo = objectMapper.readValue(topUser, QueueDetailInfo.class);  // ← RankingData로 읽기
            result.add(detailInfo);
        }

        return result;
    }

    /**
     * 토큰 검증
     */
    @Transactional
    public TokenValidateCommandDto.Response validateToken(TokenValidateCommandDto.Request request) {
        // 토큰 암호화
        String encryptedToken = cryptoUtils.encrypt(request.getToken());

        Optional<QueueEntity> foundQueue = queueRepository.findByUserIdAndConcertScheduleIdAndEncryptedToken(
                request.getUserId(),
                request.getConcertScheduleId(),
                encryptedToken
        );

        if (foundQueue.isEmpty()) {
            return TokenValidateCommandDto.Response.builder()
                    .isValid(Boolean.FALSE)
                    .status(TokenStatus.INVALID.name())
                    .message(MessageCode.TOKEN_IS_NOT_VALID.getMessage())
                    .build();
        }

        QueueEntity queue = foundQueue.get();

        // 만료 확인
        if (queue.isTokenExpired()) {
            queue.expireToken();
            queueRepository.save(queue);

            return TokenValidateCommandDto.Response.builder()
                    .isValid(Boolean.FALSE)
                    .status(TokenStatus.EXPIRED.name())
                    .message(MessageCode.TOKEN_EXPIRED.format(queue.getExpiresAt()))
                    .build();
        }

        log.info("사용자 {}의 토큰 검증 성공", request.getUserId());

        return TokenValidateCommandDto.Response.builder()
                .isValid(Boolean.TRUE)
                .status(TokenStatus.VALID.name())
                .message(MessageCode.TOKEN_VALID.getMessage())
                .expiresAt(queue.getExpiresAt())
                .build();
    }
}