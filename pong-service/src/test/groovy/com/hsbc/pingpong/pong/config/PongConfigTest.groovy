package com.hsbc.pingpong.pong.config

import com.hsbc.pingpong.pong.service.PingPongEventPublisher
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

class PongConfigTest extends Specification {

    def "defines a kafka event publisher bean from the configured topic"() {
        given:
        def config = new PongKafkaConfig()
        def properties = new PongProperties()
        properties.kafka.topic = "ping-pong-events"
        properties.kafka.enabled = true
        def kafkaTemplate = Mock(KafkaTemplate)

        when:
        def publisher = config.pingPongEventPublisher(kafkaTemplate, properties, "pong-1")

        then:
        publisher instanceof PingPongEventPublisher
    }
}
