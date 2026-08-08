package com.hsbc.pingpong.pong.service

import com.hsbc.pingpong.pong.controller.PongController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

import java.util.function.LongSupplier

class PongControllerSpec extends Specification {

    long now = 1000L
    TokenBucket tokenBucket
    WebTestClient client

    def setup() {
        tokenBucket = new TokenBucket(1, 1, { now } as LongSupplier)
        client = WebTestClient.bindToController(new PongController(tokenBucket)).build()
    }

    def "returns 200 World for the first request in a second"() {
        when:
        def response = client.post().uri("/api/pong/ping")
                .header("X-Ping-Instance", "ping-8011")
                .contentType(MediaType.TEXT_PLAIN).bodyValue("Hello").exchange()

        then:
        response.expectStatus().isOk()
                .expectBody(String).isEqualTo("World")
    }

    def "returns 429 for a second request within the same second"() {
        given:
        client.post().uri("/api/pong/ping")
                .contentType(MediaType.TEXT_PLAIN).bodyValue("Hello").exchange()

        when:
        def response = client.post().uri("/api/pong/ping")
                .contentType(MediaType.TEXT_PLAIN).bodyValue("Hello").exchange()

        then:
        response.expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    def "recovers in the next second"() {
        given:
        client.post().uri("/api/pong/ping")
                .contentType(MediaType.TEXT_PLAIN).bodyValue("Hello").exchange()
        client.post().uri("/api/pong/ping")
                .contentType(MediaType.TEXT_PLAIN).bodyValue("Hello").exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        now = 2000L

        when:
        def response = client.post().uri("/api/pong/ping")
                .contentType(MediaType.TEXT_PLAIN).bodyValue("Hello").exchange()

        then:
        response.expectStatus().isOk()
                .expectBody(String).isEqualTo("World")
    }
}
