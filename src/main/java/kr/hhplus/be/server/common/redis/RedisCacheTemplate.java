package kr.hhplus.be.server.common.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class RedisCacheTemplate {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisCacheTemplate(
            @Qualifier("cacheRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 캐시에서 데이터 조회, 없으면 supplier로 데이터 생성 후 캐시 저장
     */
    public <T> T getOrSet(String key, Supplier<T> dataSupplier, Duration ttl, Class<T> clazz) {
        try {
            // 1. 캐시에서 조회
            Object cachedValue = redisTemplate.opsForValue().get(key);

            if (cachedValue != null) {
                log.debug("Cache hit for key: {}", key);
                return objectMapper.convertValue(cachedValue, clazz);
            }

            // 2. 캐시 미스 - 데이터 생성
            log.debug("Cache miss for key: {}", key);
            T data = dataSupplier.get();

            if (data != null) {
                // 3. 캐시에 저장
                redisTemplate.opsForValue().set(key, data, ttl.toMillis(), TimeUnit.MILLISECONDS);
                log.debug("Data cached for key: {} with TTL: {}", key, ttl);
            }

            return data;

        } catch (Exception e) {
            log.error("Cache operation failed for key: {}, falling back to data supplier", key, e);
            // 캐시 실패 시 원본 데이터 반환
            return dataSupplier.get();
        }
    }

    /**
     * List나 복잡한 객체를 위한 TypeReference 버전
     */
    public <T> T getOrSet(String key, Supplier<T> dataSupplier, Duration ttl, TypeReference<T> typeRef) {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(key);

            if (cachedValue != null) {
                log.debug("Cache hit for key: {}", key);
                return objectMapper.convertValue(cachedValue, typeRef);
            }

            log.debug("Cache miss for key: {}", key);
            T data = dataSupplier.get();

            if (data != null) {
                redisTemplate.opsForValue().set(key, data, ttl.toMillis(), TimeUnit.MILLISECONDS);
                log.debug("Data cached for key: {} with TTL: {}", key, ttl);
            }

            return data;

        } catch (Exception e) {
            log.error("Cache operation failed for key: {}, falling back to data supplier", key, e);
            return dataSupplier.get();
        }
    }

    /**
     * 단순 조회
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(key);
            return cachedValue != null ? objectMapper.convertValue(cachedValue, clazz) : null;
        } catch (Exception e) {
            log.error("Failed to get cache for key: {}", key, e);
            return null;
        }
    }

    /**
     * 캐시 저장
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Data cached for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Failed to set cache for key: {}", key, e);
        }
    }

    /**
     * 캐시 삭제
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache deleted for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete cache for key: {}", key, e);
        }
    }

    /**
     * 키 존재 여부 확인
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check cache existence for key: {}", key, e);
            return false;
        }
    }

    // 기본 TTL 사용하는 편의 메소드들
    public <T> T getOrSet(String key, Supplier<T> dataSupplier, Class<T> clazz) {
        return getOrSet(key, dataSupplier, Duration.ofMinutes(10), clazz);
    }

    public void set(String key, Object value) {
        set(key, value, Duration.ofMinutes(10));
    }

    // ZSet 관련 메서드 추가
    // WANING! : member를 넘겨줘도 알아서 잘 redisTemplate 에서 직렬화해준다.
    public void addToSortedSet(String key, Object member, double score) {
        try {
            redisTemplate.opsForZSet().add(key, member, score);

            // 자정까지 TTL 설정
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
            Duration ttl = Duration.between(now, nextMidnight);
            redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);

            log.debug("Added to sorted set - key: {}, member: {}, score: {}", key, member, score);
        } catch (Exception e) {
            log.error("Failed to add to sorted set for key: {}", key, e);
        }
    }

    public Set<String> getReverseRangeFromSortedSet(String key, long start, long end) {
        try {
            return stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.error("Failed to get reverse range from sorted set for key: {}", key, e);
            return Collections.emptySet();
        }
    }

    public Long getReverseRankFromSortedSet(String key, String member) {
        try {
            return redisTemplate.opsForZSet().reverseRank(key, member);
        } catch (Exception e) {
            log.error("Failed to get reverse rank from sorted set for key: {}", key, e);
            return null;
        }
    }

    // 원자적 감소 연산 추가
    public Long decrement(String key) {
        try {
            if (!redisTemplate.hasKey(key)) {
                log.error("Redis key not found: {} - 배치 초기화 확인 필요", key);
                return null;
            }

            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Failed to decrement for key: {}", key, e);
            return null;
        }
    }

    public Long incrementRemaining(String remainingKey) {
        try {
            return redisTemplate.opsForValue().increment(remainingKey);
        } catch (Exception e) {
            log.error("Failed to increment remains for key: {}", remainingKey, e);
            return null;
        }
    }
}
