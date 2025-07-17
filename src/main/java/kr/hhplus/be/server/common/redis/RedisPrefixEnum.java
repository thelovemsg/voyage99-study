package kr.hhplus.be.server.common.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisPrefixEnum {

    COLON(":"),
    LOCK("lock"),
    TICKET("ticket"),
    RESERVE("reserve"),
    PURCHASE("purchase"),
    INFO("info"),
    USER("user"),
    REMAINING("remaining"),
    SCHEDULE("schedule"),
    RANK("rank"),
    DAILY("daily"),
    QUEUE("queue")
    ;

    private final String prefix;

    public static RedisPrefixEnum findByPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }

        for (RedisPrefixEnum redisPrefixEnum : values()) {
            if (redisPrefixEnum.prefix.equals(prefix)) {
                return redisPrefixEnum;
            }
        }
        return null;
    }

}
