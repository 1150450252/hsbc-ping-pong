package com.hsbc.pingpong.ping.config

import spock.lang.Specification

class PingPropertiesSpec extends Specification {

    def "holds sensible default values"() {
        given:
        def properties = new PingProperties()

        expect:
        properties.instanceId == "ping"
        properties.pongUrl == "http://localhost:8033"
        properties.rateLimit.type == "redis-lua"
        properties.rateLimit.maxTokens == 2
        properties.rateLimit.refillPerSecond == 2
        properties.rateLimit.step == 1
        properties.rateLimit.key == "ping-pong-rate-limit"
        properties.rateLimit.lockFile == null
        properties.kafka.enabled == true
        properties.kafka.topic == "ping-pong-events"
        properties.kafka.groupId == "ping-service"
        properties.kafka.maxRetries == 3
        properties.kafka.dltTopic == "ping-pong-events-dlt"
        properties.kafka.retryBackoffMs == 1000
    }

    def "exposes setters for all nested configuration"() {
        given:
        def properties = new PingProperties()

        when:
        properties.instanceId = "ping-2"
        properties.pongUrl = "http://localhost:9090"
        properties.rateLimit.type = "file-lock"
        properties.rateLimit.maxTokens = 5
        properties.rateLimit.refillPerSecond = 10
        properties.rateLimit.step = 2
        properties.rateLimit.key = "my-bucket"
        properties.rateLimit.lockFile = "/tmp/my-bucket.lock"
        properties.kafka.enabled = false
        properties.kafka.topic = "other-events"
        properties.kafka.groupId = "consumer-a"
        properties.kafka.maxRetries = 7
        properties.kafka.dltTopic = "custom-dlt"
        properties.kafka.retryBackoffMs = 2000

        then:
        properties.instanceId == "ping-2"
        properties.pongUrl == "http://localhost:9090"
        properties.rateLimit.type == "file-lock"
        properties.rateLimit.maxTokens == 5
        properties.rateLimit.refillPerSecond == 10
        properties.rateLimit.step == 2
        properties.rateLimit.key == "my-bucket"
        properties.rateLimit.lockFile == "/tmp/my-bucket.lock"
        properties.kafka.enabled == false
        properties.kafka.topic == "other-events"
        properties.kafka.groupId == "consumer-a"
        properties.kafka.maxRetries == 7
        properties.kafka.dltTopic == "custom-dlt"
        properties.kafka.retryBackoffMs == 2000
    }
}
