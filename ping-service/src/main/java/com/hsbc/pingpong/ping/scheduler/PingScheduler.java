package com.hsbc.pingpong.ping.scheduler;

import com.hsbc.pingpong.ping.service.PingRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PingScheduler {

    private final PingRequestService pingRequestService;

    @Scheduled(fixedRate = 500)
    public void pingOnce() {
        pingRequestService.fireOnce().subscribe();
    }
}
