package com.hsbc.pingpong.ping.service

import com.hsbc.pingpong.common.event.PingPongResult
import com.hsbc.pingpong.ping.client.PingClient
import com.hsbc.pingpong.ping.event.PingEventPublisher
import com.hsbc.pingpong.ping.ratelimit.RateLimiter
import reactor.core.publisher.Mono
import spock.lang.Specification

class PingRequestServiceSpec extends Specification {

    RateLimiter rateLimiter
    PingClient pingClient
    PingEventPublisher eventPublisher
    PingRequestService service

    def setup() {
        rateLimiter = Mock(RateLimiter)
        pingClient = Mock(PingClient)
        eventPublisher = Mock(PingEventPublisher)
        service = new PingRequestService(rateLimiter, pingClient, eventPublisher, "ping-1")
    }

    def "records a locally rate limited result when the limiter denies"() {
        given:
        rateLimiter.tryAcquire() >> false

        when:
        def result = service.fireOnce().block()

        then:
        result == PingPongResult.RATE_LIMITED_LOCALLY
        0 * pingClient.ping()
        1 * eventPublisher.publish(PingPongResult.RATE_LIMITED_LOCALLY)
    }

    def "records sent and pong responded when the limiter allows and pong returns 200"() {
        given:
        rateLimiter.tryAcquire() >> true
        pingClient.ping() >> Mono.just(200)

        when:
        def result = service.fireOnce().block()

        then:
        result == PingPongResult.SENT_PONG_RESPONDED
        1 * eventPublisher.publish(PingPongResult.SENT_PONG_RESPONDED)
    }

    def "records sent and pong throttled when pong returns 429"() {
        given:
        rateLimiter.tryAcquire() >> true
        pingClient.ping() >> Mono.just(429)

        when:
        def result = service.fireOnce().block()

        then:
        result == PingPongResult.SENT_PONG_THROTTLED
        1 * eventPublisher.publish(PingPongResult.SENT_PONG_THROTTLED)
    }

    def "records sent and pong throttled when the ping request fails"() {
        given:
        rateLimiter.tryAcquire() >> true
        pingClient.ping() >> Mono.error(new RuntimeException("boom"))

        when:
        def result = service.fireOnce().block()

        then:
        result == PingPongResult.SENT_PONG_THROTTLED
        1 * eventPublisher.publish(PingPongResult.SENT_PONG_THROTTLED)
    }
}
