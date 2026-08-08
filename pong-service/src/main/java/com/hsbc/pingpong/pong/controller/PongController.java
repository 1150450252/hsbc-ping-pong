package com.hsbc.pingpong.pong.controller;

import com.hsbc.pingpong.pong.service.TokenBucket;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pong")
@RequiredArgsConstructor
public class PongController {

    private static final Logger log = LoggerFactory.getLogger(PongController.class);

    private final TokenBucket tokenBucket;

    @PostMapping("/ping")
    public Mono<ResponseEntity<String>> ping(@RequestBody String message,
                                             @RequestHeader(value = "X-Ping-Instance", required = false, defaultValue = "unknown") String pingInstance) {
        if (tokenBucket.tryAcquire()) {
            log.info("Received '{}' from {} -> 200", message, pingInstance);
            return Mono.just(ResponseEntity.ok("World"));
        }
        log.info("Received '{}' from {} -> 429", message, pingInstance);
        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Pong throttled: rate limit exceeded (1 req/sec)"));
    }
}
