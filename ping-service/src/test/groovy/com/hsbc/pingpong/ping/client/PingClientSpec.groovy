package com.hsbc.pingpong.ping.client

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

class PingClientSpec extends Specification {

    MockWebServer server
    PingClient client

    def setup() {
        server = new MockWebServer()
        server.start()
        client = new PingClient(server.url("/").toString(), "ping-8011")
    }

    def cleanup() {
        server.shutdown()
    }

    def "sends Hello to Pong and returns 200 World when Pong responds"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(200).setBody("World"))

        when:
        def reply = client.ping().block()

        then:
        reply.status == 200
        reply.body == "World"
        def request = server.takeRequest()
        request.method == "POST"
        request.path == "/api/pong/ping"
        request.getHeader("X-Ping-Instance") == "ping-8011"
        request.body.readUtf8() == "Hello"
    }

    def "returns 429 when Pong throttles the request"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(429).setBody("Pong throttled"))

        when:
        def reply = client.ping().block()

        then:
        reply.status == 429
        reply.body == "Pong throttled"
    }
}
