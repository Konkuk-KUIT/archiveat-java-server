package com.archiveat.server.global.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 기반 분산 락 서비스
 * 
 * Watchdog 기능:
 * - leaseTime을 -1로 설정하면 Redisson이 자동으로 락을 갱신
 * - 서버가 비정상 종료되면 30초 내에 락이 자동 해제됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public abstract class DistributedLockService {

    private final RedissonClient redissonClient;

    public abstract boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit);

    /**
     * 분산 락 획득 시도
     * 
     * @param key      락 키
     * @param waitTime 대기 시간 (초)
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String key, long waitTime) {
        RLock lock = redissonClient.getLock(key);
        try {
            // leaseTime을 -1로 설정하여 Watchdog 활성화
            boolean acquired = lock.tryLock(waitTime, -1, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Lock acquired: {}", key);
            } else {
                log.warn("Failed to acquire lock: {}", key);
            }
            return acquired;
        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted: {}", key, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 분산 락 해제
     * 
     * @param key 락 키
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released: {}", key);
        } else {
            log.warn("Attempted to unlock a lock not held by current thread: {}", key);
        }
    }

    /**
     * 락이 잡혀있는지 확인
     * 
     * @param key 락 키
     * @return 락 보유 여부
     */
    public boolean isLocked(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.isLocked();
    }
}
