package com.hsbc.pingpong.common.event

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class PingPongCommonSpec extends Specification {

    def "has exactly the three ping-side challenge result categories"() {
        expect:
        PingPongResult.values().length == 3
    }

    def "every enum value carries a non-blank description"() {
        expect:
        PingPongResult.values().every { result ->
            result.description != null && !result.description.trim().isEmpty()
        }
    }

    def "every enum value carries a non-blank Chinese description"() {
        expect:
        PingPongResult.values().every { result ->
            result.descriptionZh != null && !result.descriptionZh.trim().isEmpty()
        }
    }

    def "Chinese descriptions reflect the challenge result categories"() {
        expect:
        PingPongResult.RATE_LIMITED_LOCALLY.descriptionZh.contains("限流")
        PingPongResult.SENT_PONG_RESPONDED.descriptionZh.contains("已发送")
        PingPongResult.SENT_PONG_THROTTLED.descriptionZh.contains("已发送")
    }

    def "enum descriptions reflect the challenge result categories"() {
        expect:
        PingPongResult.RATE_LIMITED_LOCALLY.description.contains("not sent")
        PingPongResult.SENT_PONG_RESPONDED.description.contains("responded")
        PingPongResult.SENT_PONG_THROTTLED.description.contains("throttled")
    }

    def "event POJO exposes all properties through the all-args constructor and getters"() {
        given:
        def event = new PingPongEvent("ping-1", "PING", PingPongResult.SENT_PONG_RESPONDED, 123L)

        expect:
        event.instanceId == "ping-1"
        event.source == "PING"
        event.result == PingPongResult.SENT_PONG_RESPONDED
        event.timestamp == 123L
    }

    def "event POJO properties can be updated through setters"() {
        given:
        def event = new PingPongEvent()

        when:
        event.instanceId = "ping-2"
        event.source = "PONG"
        event.result = PingPongResult.SENT_PONG_RESPONDED
        event.timestamp = 456L

        then:
        event.instanceId == "ping-2"
        event.source == "PONG"
        event.result == PingPongResult.SENT_PONG_RESPONDED
        event.timestamp == 456L
    }

    def "event supports value equality, hashing and a readable toString"() {
        given:
        def a = new PingPongEvent("ping-1", "PING", PingPongResult.SENT_PONG_RESPONDED, 123L)
        def b = new PingPongEvent("ping-1", "PING", PingPongResult.SENT_PONG_RESPONDED, 123L)
        def c = new PingPongEvent("ping-2", "PING", PingPongResult.SENT_PONG_RESPONDED, 123L)

        expect:
        a == b
        b == a
        a.hashCode() == b.hashCode()
        a != c
        a.toString().contains("ping-1")
    }

    def "event serializes and deserializes through Jackson"() {
        given:
        def mapper = new ObjectMapper()
        def original = new PingPongEvent("ping-3", "PING", PingPongResult.RATE_LIMITED_LOCALLY, 789L)

        when:
        def json = mapper.writeValueAsString(original)
        def roundTripped = mapper.readValue(json, PingPongEvent)

        then:
        roundTripped.instanceId == "ping-3"
        roundTripped.source == "PING"
        roundTripped.result == PingPongResult.RATE_LIMITED_LOCALLY
        roundTripped.timestamp == 789L
    }
}
