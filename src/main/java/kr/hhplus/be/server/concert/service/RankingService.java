package kr.hhplus.be.server.concert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
import kr.hhplus.be.server.concert.controller.handler.RankingData;
import kr.hhplus.be.server.concert.controller.handler.RankingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisCacheTemplate cacheTemplate;
    private final ObjectMapper objectMapper;

    public void addToRanking(Long concertScheduleId, String concertTitle, LocalDateTime soldOutTime) {
        String rankingKey = RedisKeyUtils.getDailyRankingKey();

        RankingData rankingData = RankingData.builder()
                .concertScheduleId(concertScheduleId)
                .concertTitle(concertTitle)
                .soldOutTime(soldOutTime)
                .build();

        double score = calculateScore(soldOutTime);

        cacheTemplate.addToSortedSet(rankingKey, rankingData, score);
    }

    /**
     * Redis의 경우 sorted set의 경우 점수가 기본적으로 오름차순으로 저장됨.
     * calculateScore의 경우 크면 클수록 빠르기 때문에, ranking 을 찾아올 때 조심해야함.
     */
    public List<RankingInfo> getTodayRanking(int limit) throws JsonProcessingException {
        String rankingKey = RedisKeyUtils.getDailyRankingKey();

        // 점수 높은 순으로 조회 (빠른 매진 순) => Set은 기본적으로 순서를 보장하지 않지만 redis 에서 내부적으로 LinkedHashSet을 사용해서 반환함.
        Set<String> rankingJsons = cacheTemplate.getReverseRangeFromSortedSet(rankingKey, 0, limit - 1);

        List<RankingInfo> rankings = new ArrayList<>();

        int rank = 1;

        for (String json : rankingJsons) {
            log.info("json : {}", json);
            RankingData data = objectMapper.readValue(json, RankingData.class);  // ← RankingData로 읽기

            RankingInfo info = RankingInfo.builder()
                    .concertScheduleId(data.getConcertScheduleId())
                    .concertTitle(data.getConcertTitle())
                    .soldOutTime(data.getSoldOutTime())
                    .rank(rank++)
                    .build();

            rankings.add(info);
        }

        return rankings;
    }

    private double calculateScore(LocalDateTime soldOutTime) {
        // 자정부터 매진 시점까지 경과 시간 계산
        LocalDateTime midnight = soldOutTime.toLocalDate().atStartOfDay();
        long secondsFromMidnight = Duration.between(midnight, soldOutTime).getSeconds();

        // 빠를수록 높은 점수 (86400초 = 24시간)
        return 86400.0 - secondsFromMidnight;
    }
}
