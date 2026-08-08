package com.hsbc.pingpong.pong.config

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DefaultErrorHandler
import spock.lang.Specification

class PongKafkaConfigSpec extends Specification {

    def "wires a container factory with a dead-letter error handler"() {
        given:
        def config = new PongKafkaConfig()
        def consumerFactory = Mock(ConsumerFactory)
        def kafkaTemplate = Mock(KafkaTemplate)
        def properties = new PongProperties()

        when:
        def factory = config.kafkaListenerContainerFactory(consumerFactory, kafkaTemplate, properties)

        then:
        factory instanceof ConcurrentKafkaListenerContainerFactory
        factory.getCommonErrorHandler() instanceof DefaultErrorHandler
    }
}
