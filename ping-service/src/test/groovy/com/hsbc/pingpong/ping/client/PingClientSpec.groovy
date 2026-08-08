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
        client = new PingClient(server.url("/").toString())
    }

    def cleanup() {
        server.shutdown()
    }

    def "returns 200 when Pong responds"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(200).setBody("Pong"))

        when:
        def status = client.ping().block()

        then:
        status == 200
    }

    def "returns 429 when Pong throttles the request"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(429).setBody("Pong throttled"))

        when:
        def status = client.ping().block()

        then:
        status == 429
    }
}
