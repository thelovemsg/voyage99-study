package kr.hhplus.be.server.redis.controller.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisDto {

    @Getter
    @Setter
    public static class Request {
        private String key;
        private String value;
    }
}
