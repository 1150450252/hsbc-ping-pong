package com.hsbc.pingpong.ping.service;

import com.hsbc.pingpong.common.event.PingPongResult;
import com.hsbc.pingpong.ping.client.PingClient;
import com.hsbc.pingpong.ping.client.PingResultClassifier;
import com.hsbc.pingpong.ping.event.PingEventPublisher;
import com.hsbc.pingpong.ping.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Shared ping-triggering logic, used by both the scheduled PingScheduler
 * and the on-demand PingController REST endpoint.
 */
@Service
public class PingRequestService {

    private static final Logger log = LoggerFactory.getLogger(PingRequestService.class);

    private final RateLimiter rateLimiter;
    private final PingClient pingClient;
    private final PingEventPublisher eventPublisher;
    private final String instanceId;

    public PingRequestService(RateLimiter rateLimiter, PingClient pingClient, PingEventPublisher eventPublisher,
                              @Value("${hsbc.pingpong.instance-id:ping-${server.port:8011}}") String instanceId) {
        this.rateLimiter = rateLimiter;
        this.pingClient = pingClient;
        this.eventPublisher = eventPublisher;
        this.instanceId = instanceId;
    }

    public Mono<PingPongResult> fireOnce() {
        if (!rateLimiter.tryAcquire()) {
            return Mono.just(publish(PingPongResult.RATE_LIMITED_LOCALLY, null, null));
        }
        return pingClient.ping()
                .map(reply -> publish(PingResultClassifier.classify(false, reply.getStatus()), reply.getStatus(), reply.getBody()))
                .onErrorResume(e -> {
                    log.warn("[{}] Ping request failed: {}", instanceId, e.getMessage());
                    return Mono.just(publish(PingPongResult.SENT_PONG_THROTTLED, null, null));
                });
    }

    private PingPongResult publish(PingPongResult result, Integer status, String reply) {
        log.info("[{}] [status={}] [reply={}] {}", instanceId, status == null ? "--" : status,
                reply == null ? "--" : reply, result.getDescription());
        eventPublisher.publish(result);
        return result;
    }
}
