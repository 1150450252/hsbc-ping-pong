package com.hsbc.pingpong.ping.controller;

import lombok.RequiredArgsConstructor;
import com.hsbc.pingpong.common.event.PingPongResult;
import com.hsbc.pingpong.ping.service.PingRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * On-demand trigger for a single ping-pong request, handy for demos and
 * debugging. The scheduled PingScheduler drives the same logic every second.
 */
@RestController
@RequestMapping("/api/ping")
@RequiredArgsConstructor
public class PingController {

    private final PingRequestService pingRequestService;


    @GetMapping("/trigger")
    public Mono<ResponseEntity<String>> trigger() {
        return pingRequestService.fireOnce().map(PingController::toResponse);
    }

    private static ResponseEntity<String> toResponse(PingPongResult result) {
        switch (result) {
            case SENT_PONG_RESPONDED:
                return ResponseEntity.ok(result.getDescription());
            default:
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result.getDescription());
        }
    }
}
