package kr.hhplus.be.server.redis.controller;

import kr.hhplus.be.server.redis.controller.dto.RedisDto;
import kr.hhplus.be.server.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;

    @GetMapping("/redis")
    public String getRedis(@RequestBody RedisDto.Request request) {
        return redisService.getRedis(request);
    }
}
