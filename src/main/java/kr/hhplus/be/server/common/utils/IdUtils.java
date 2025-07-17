package kr.hhplus.be.server.common.utils;

import io.hypersistence.tsid.TSID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IdUtils {

    public static Long getNewId() {
        return TSID.fast().toLong();
    }
}
