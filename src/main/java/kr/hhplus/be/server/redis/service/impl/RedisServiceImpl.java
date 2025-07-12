package kr.hhplus.be.server.redis.service.impl;

import kr.hhplus.be.server.redis.controller.dto.RedisDto;
import kr.hhplus.be.server.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String getRedis(RedisDto.Request request) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        String result = (String) operations.get(request.getKey());
        if (!StringUtils.hasText(result)) {
            operations.set(request.getKey(), request.getValue(), 10, TimeUnit.MINUTES);
            log.info("redis save");
            result = request.getValue();
        }
        return result;
    }
}
