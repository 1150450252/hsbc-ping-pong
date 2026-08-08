package com.hsbc.pingpong.ping.scheduler;

import com.hsbc.pingpong.common.event.PingPongResult;
import com.hsbc.pingpong.ping.service.PingRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PingScheduler {

    private static final int MAX_JITTER_MS = 50;

    private final PingRequestService pingRequestService;
    private final long maxJitterMs;

    @Autowired
    public PingScheduler(PingRequestService pingRequestService) {
        this(pingRequestService, MAX_JITTER_MS);
    }

    PingScheduler(PingRequestService pingRequestService, long maxJitterMs) {
        this.pingRequestService = pingRequestService;
        this.maxJitterMs = maxJitterMs;
    }

    @Scheduled(fixedRate = 1000)
    public void pingOnce() {
        fire().subscribe();
    }

    Mono<PingPongResult> fire() {
        //这块为了模拟下网络抖动，自己测试用的
//        long jitterMs = ThreadLocalRandom.current().nextLong(maxJitterMs);
        long jitterMs = 0;
        return Mono.delay(Duration.ofMillis(jitterMs))
                .flatMap(ignored -> pingRequestService.fireOnce());
    }
}
