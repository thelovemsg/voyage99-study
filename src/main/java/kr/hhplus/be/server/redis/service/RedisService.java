package kr.hhplus.be.server.redis.service;

import kr.hhplus.be.server.redis.controller.dto.RedisDto;

public interface RedisService {
    String getRedis(RedisDto.Request request);
}
