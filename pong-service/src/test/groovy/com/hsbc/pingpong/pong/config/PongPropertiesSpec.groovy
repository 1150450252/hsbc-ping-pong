package com.hsbc.pingpong.pong.config

import spock.lang.Specification

class PongPropertiesSpec extends Specification {

    def "holds sensible default values"() {
        given:
        def properties = new PongProperties()

        expect:
        properties.maxRequestsPerSecond == 1
        properties.kafka.enabled
        properties.kafka.topic == "ping-pong-events"
        properties.kafka.groupId == "pong-service"
        properties.kafka.maxRetries == 3
        properties.kafka.dltTopic == "ping-pong-events-dlt"
        properties.kafka.retryBackoffMs == 1000
    }

    def "exposes setters for all nested configuration"() {
        given:
        def properties = new PongProperties()

        when:
        properties.maxRequestsPerSecond = 5
        properties.kafka.enabled = false
        properties.kafka.topic = "custom-events"
        properties.kafka.groupId = "consumer-a"
        properties.kafka.maxRetries = 7
        properties.kafka.dltTopic = "custom-dlt"
        properties.kafka.retryBackoffMs = 2000

        then:
        properties.maxRequestsPerSecond == 5
        !properties.kafka.enabled
        properties.kafka.topic == "custom-events"
        properties.kafka.groupId == "consumer-a"
        properties.kafka.maxRetries == 7
        properties.kafka.dltTopic == "custom-dlt"
        properties.kafka.retryBackoffMs == 2000
    }

    def "allows replacing the nested kafka configuration"() {
        given:
        def properties = new PongProperties()
        def kafka = new PongProperties.Kafka()
        kafka.topic = "replacement-topic"

        when:
        properties.kafka = kafka

        then:
        properties.kafka.is(kafka)
    }

    def "supports value equality, hashing and a readable toString"() {
        given:
        def a = new PongProperties()
        def b = new PongProperties()
        def c = new PongProperties()
        c.maxRequestsPerSecond = 9

        expect:
        a == b
        a.hashCode() == b.hashCode()
        a != c
        a.toString().contains("maxRequestsPerSecond")
        a.kafka.toString().contains("ping-pong-events")
    }
}
