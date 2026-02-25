package com.beyond.pochaon.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
//락 최대 유지 시간 설정
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
public class SchedulerConfig {

    @Bean
    @Qualifier("taskSchedular")
    public ThreadPoolTaskScheduler taskScheduler() {
//      ThreadPoolTaskScheduler 작업을 동시에 여러개 처리할 수 있게 해주는 클래스
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//        쓰레드 10개 할당한다는 의미
        scheduler.setPoolSize(10);
//        작업이 끝난 쓰레드들 정리
        scheduler.initialize();
        return scheduler;
    }

//    어느 서버가 스케쥴러를 돌리고 있는지 기록하는 메서드
    @Bean
    public LockProvider lockProvider(@Qualifier("schedularRedis") RedisConnectionFactory connectionFactory) {
//        redis를 이용한 분산 락 설정
        return new RedisLockProvider(connectionFactory);

    }
}
