package com.hsbc.pingpong.ping.event

import com.hsbc.pingpong.common.event.PingPongEvent
import com.hsbc.pingpong.common.event.PingPongResult
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

class PingEventPublisherSpec extends Specification {

    def "publishes an event to the configured topic when enabled"() {
        given:
        def kafkaTemplate = Mock(KafkaTemplate)
        def publisher = new PingEventPublisher(kafkaTemplate, "ping-pong-events", "ping-1", true)

        when:
        publisher.publish(PingPongResult.SENT_PONG_RESPONDED)

        then:
        1 * kafkaTemplate.send("ping-pong-events", { PingPongEvent event ->
            event.instanceId == "ping-1"
            event.source == "PING"
            event.result == PingPongResult.SENT_PONG_RESPONDED
        })
    }

    def "swallows an exception thrown while sending to Kafka"() {
        given:
        def kafkaTemplate = Mock(KafkaTemplate)
        def publisher = new PingEventPublisher(kafkaTemplate, "ping-pong-events", "ping-1", true)

        when:
        publisher.publish(PingPongResult.SENT_PONG_RESPONDED)

        then:
        1 * kafkaTemplate.send("ping-pong-events", _) >> { throw new RuntimeException("kafka down") }
        notThrown(Exception)
    }

    def "does nothing when disabled"() {
        given:
        def kafkaTemplate = Mock(KafkaTemplate)
        def publisher = new PingEventPublisher(kafkaTemplate, "ping-pong-events", "ping-1", false)

        when:
        publisher.publish(PingPongResult.RATE_LIMITED_LOCALLY)

        then:
        0 * kafkaTemplate.send(*_)
    }
}
