package kr.hhplus.be.server.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLockTemplate {

    private final RedissonClient redissonClient;

    /**
     * 분산 락을 사용하여 작업을 실행합니다.
     *
     * @param lockKey 락 키
     * @param task 실행할 작업
     * @param <T> 반환 타입
     * @param maxRetries 최대 재시도 횟수
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime 락 유지 시간
     * @param timeUnit 시간 단위
     * @param exceptionSupplier 락 획득 실패 시 발생시킬 예외
     * @return 작업 결과
     */
    public <T, E extends Exception> T executeWithLock(
            String lockKey,
            Supplier<T> task,
            int maxRetries,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<E> exceptionSupplier) throws E {

        RLock lock = redissonClient.getLock(lockKey);
        int retryCount = 0;

        while (retryCount < maxRetries) {
            boolean lockAcquired = false;
            try {
                lockAcquired = lock.tryLock(waitTime, leaseTime, timeUnit);
                if (lockAcquired) {
                    return task.get();
                }

                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        // 재시도 간 약간의 지연
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Lock acquisition interrupted", e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock acquisition interrupted", e);
            } finally {
                if (lockAcquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("Lock released: {}", lockKey); // 디버깅용
                }
            }
        }

        // 모든 재시도 실패 시 예외 발생
        throw exceptionSupplier.get();
    }

    // 더 간단한 오버로드 메소드들 추가
    public <T> T executeWithLock(
            String lockKey,
            Supplier<T> task,
            int maxRetries,
            Supplier<RuntimeException> exceptionSupplier) {
        return executeWithLock(lockKey, task, maxRetries, 3, 30, TimeUnit.SECONDS, exceptionSupplier);
    }

    public <T> T executeWithLock(
            String lockKey,
            Supplier<T> task,
            Supplier<RuntimeException> exceptionSupplier) {
        return executeWithLock(lockKey, task, 3, 3, 30, TimeUnit.SECONDS, exceptionSupplier);
    }

    // 기본값을 사용하는 가장 간단한 버전
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(
                lockKey,
                task,
                3,
                3,
                30,
                TimeUnit.SECONDS,
                () -> new RuntimeException("Failed to acquire lock after retries")
        );
    }
}
