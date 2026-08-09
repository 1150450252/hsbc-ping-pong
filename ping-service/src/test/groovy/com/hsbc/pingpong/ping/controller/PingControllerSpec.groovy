package com.hsbc.pingpong.ping.controller

import com.hsbc.pingpong.common.event.PingPongResult
import com.hsbc.pingpong.ping.service.PingRequestService
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import spock.lang.Specification

class PingControllerSpec extends Specification {

    PingRequestService pingRequestService
    PingController controller

    def setup() {
        pingRequestService = Mock(PingRequestService)
        controller = new PingController(pingRequestService)
    }

    def "returns 200 when pong responded"() {
        given:
        pingRequestService.fireOnce() >> Mono.just(PingPongResult.SENT_PONG_RESPONDED)

        expect:
        controller.trigger().block().statusCode == HttpStatus.OK
    }

    def "returns 503 when the request was rate limited locally"() {
        given:
        pingRequestService.fireOnce() >> Mono.just(PingPongResult.RATE_LIMITED_LOCALLY)

        expect:
        controller.trigger().block().statusCode == HttpStatus.SERVICE_UNAVAILABLE
    }

    def "returns 429 when pong throttled the request"() {
        given:
        pingRequestService.fireOnce() >> Mono.just(PingPongResult.SENT_PONG_THROTTLED)

        expect:
        controller.trigger().block().statusCode == HttpStatus.TOO_MANY_REQUESTS
    }
}
