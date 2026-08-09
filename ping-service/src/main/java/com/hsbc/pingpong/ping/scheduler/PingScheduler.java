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

    private final PingRequestService pingRequestService;

    @Autowired
    public PingScheduler(PingRequestService pingRequestService) {
        this.pingRequestService = pingRequestService;
    }

    @Scheduled(fixedRate = 1000)
    public void pingOnce() {
        fire().subscribe();
    }

    Mono<PingPongResult> fire() {
        // 模拟网络抖动(0~100ms),让多实例的到达顺序随机、都有争抢令牌的机会
        long jitterMs = ThreadLocalRandom.current().nextLong(100);
        return Mono.delay(Duration.ofMillis(jitterMs))
                .flatMap(ignored -> pingRequestService.fireOnce());
    }
}
