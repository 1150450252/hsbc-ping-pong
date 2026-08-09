package com.hsbc.pingpong.ping.config

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DefaultErrorHandler
import spock.lang.Specification

class PingKafkaConfigSpec extends Specification {

    def "wires a container factory with a dead-letter error handler"() {
        given:
        def config = new PingKafkaConfig()
        def consumerFactory = Mock(ConsumerFactory)
        def kafkaTemplate = Mock(KafkaTemplate)
        def properties = new PingProperties()

        when:
        def factory = config.kafkaListenerContainerFactory(consumerFactory, kafkaTemplate, properties)

        then:
        factory instanceof ConcurrentKafkaListenerContainerFactory
        commonErrorHandler(factory) instanceof DefaultErrorHandler
    }

    // spring-kafka 2.8.11 的工厂只有 setCommonErrorHandler、没有 getter,只能反射读私有字段
    private static Object commonErrorHandler(ConcurrentKafkaListenerContainerFactory factory) {
        def field = ConcurrentKafkaListenerContainerFactory.class.superclass.declaredFields
                .find { it.name == 'commonErrorHandler' }
        field.accessible = true
        field.get(factory)
    }
}
