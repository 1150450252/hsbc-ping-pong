package com.hsbc.pingpong.pong.service

import com.hsbc.pingpong.common.event.PingPongResult
import com.hsbc.pingpong.pong.controller.PongController
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

import java.util.function.LongSupplier

class PongControllerSpec extends Specification {

    long now = 1000L
    TokenBucket tokenBucket
    PingPongEventPublisher publisher
    WebTestClient client

    def setup() {
        tokenBucket = new TokenBucket(1, 1, { now } as LongSupplier)
        publisher = Mock(PingPongEventPublisher)
        client = WebTestClient.bindToController(new PongController(tokenBucket, publisher)).build()
    }

    def "returns 200 Pong for the first request in a second"() {
        when:
        def response = client.get().uri("/api/pong/ping").exchange()

        then:
        response.expectStatus().isOk()
                .expectBody(String).isEqualTo("Pong")
        1 * publisher.publish(PingPongResult.PONG_RESPONDED)
    }

    def "returns 429 for a second request within the same second"() {
        given:
        client.get().uri("/api/pong/ping").exchange()

        when:
        def response = client.get().uri("/api/pong/ping").exchange()

        then:
        response.expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        1 * publisher.publish(PingPongResult.PONG_THROTTLED)
    }

    def "recovers in the next second"() {
        given:
        client.get().uri("/api/pong/ping").exchange()
        client.get().uri("/api/pong/ping").exchange().expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        now = 2000L

        when:
        def response = client.get().uri("/api/pong/ping").exchange()

        then:
        response.expectStatus().isOk()
                .expectBody(String).isEqualTo("Pong")
    }
}
