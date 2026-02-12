package com.archiveat.server.global.config;

import com.archiveat.server.global.lock.DistributedLockService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@TestConfiguration
public class TestConfig {

    /**
     * 테스트 환경에서는 진짜 Redis를 쓰지 않고,
     * 무조건 락 획득에 성공하는 가짜(Mock) 객체를 사용합니다.
     */
    @Bean
    @Primary // 실제 빈 대신 이 녀석이 주입됨
    public DistributedLockService mockDistributedLockService() {
        return new DistributedLockService(null) {
            @Override
            public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
                return true; // 무조건 락 획득 성공!
            }

            @Override
            public boolean tryLock(String key, long waitTime) {
                return true; // 무조건 락 획득 성공!
            }

            @Override
            public void unlock(String key) {
                // 아무것도 안 함 (가짜니까 해제할 것도 없음)
            }
        };
    }
}