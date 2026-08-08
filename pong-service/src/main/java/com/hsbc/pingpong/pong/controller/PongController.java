package com.hsbc.pingpong.pong.controller;

import lombok.RequiredArgsConstructor;
import com.hsbc.pingpong.common.event.PingPongResult;
import com.hsbc.pingpong.pong.service.PingPongEventPublisher;
import com.hsbc.pingpong.pong.service.TokenBucket;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pong")
@RequiredArgsConstructor
public class PongController {

    private final TokenBucket tokenBucket;
    private final PingPongEventPublisher eventPublisher;

    @GetMapping("/ping")
    public Mono<ResponseEntity<String>> ping() {
        if (tokenBucket.tryAcquire()) {
            eventPublisher.publish(PingPongResult.PONG_RESPONDED);
            return Mono.just(ResponseEntity.ok("Pong"));
        }
        eventPublisher.publish(PingPongResult.PONG_THROTTLED);
        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Pong throttled: rate limit exceeded (1 req/sec)"));
    }
}
