package com.hsbc.pingpong.pong.service

import com.hsbc.pingpong.common.event.PingPongEvent
import com.hsbc.pingpong.common.event.PingPongResult
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

class PingPongEventPublisherSpec extends Specification {

    def "publishes an event to the configured topic when enabled"() {
        given:
        def kafkaTemplate = Mock(KafkaTemplate)
        def publisher = new PingPongEventPublisher(kafkaTemplate, "ping-pong-events", "pong-1", true)

        when:
        publisher.publish(PingPongResult.PONG_RESPONDED)

        then:
        1 * kafkaTemplate.send("ping-pong-events", { PingPongEvent event ->
            event.instanceId == "pong-1"
            event.source == "PONG"
            event.result == PingPongResult.PONG_RESPONDED
        })
    }

    def "swallows an exception thrown while sending to Kafka"() {
        given:
        def kafkaTemplate = Mock(KafkaTemplate)
        def publisher = new PingPongEventPublisher(kafkaTemplate, "ping-pong-events", "pong-1", true)

        when:
        publisher.publish(PingPongResult.PONG_RESPONDED)

        then:
        1 * kafkaTemplate.send("ping-pong-events", _) >> { throw new RuntimeException("kafka down") }
        notThrown(Exception)
    }

    def "does nothing when disabled"() {
        given:
        def kafkaTemplate = Mock(KafkaTemplate)
        def publisher = new PingPongEventPublisher(kafkaTemplate, "ping-pong-events", "pong-1", false)

        when:
        publisher.publish(PingPongResult.PONG_THROTTLED)

        then:
        0 * kafkaTemplate.send(*_)
    }
}
