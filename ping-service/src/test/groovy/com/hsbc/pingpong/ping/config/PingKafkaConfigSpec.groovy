package com.hsbc.pingpong.ping.config

import java.util.function.BiFunction
import org.apache.kafka.clients.consumer.ConsumerRecord
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

    def "dead letter recoverer routes failed records to the configured DLT topic"() {
        given:
        def config = new PingKafkaConfig()
        def consumerFactory = Mock(ConsumerFactory)
        def kafkaTemplate = Mock(KafkaTemplate)
        def properties = new PingProperties()
        def factory = config.kafkaListenerContainerFactory(consumerFactory, kafkaTemplate, properties)
        def handler = commonErrorHandler(factory) as DefaultErrorHandler
        def tracker = fieldValue(handler, handler.class, 'failureTracker')
        def recoverer = fieldValue(tracker, tracker.class, 'recoverer')
        def resolver = fieldValue(recoverer, recoverer.class, 'destinationResolver') as BiFunction
        def record = new ConsumerRecord<>('source-topic', 7, 0L, 'key', 'value')

        when:
        def partition = resolver.apply(record, new RuntimeException('boom'))

        then:
        partition.topic() == properties.kafka.dltTopic
        partition.partition() == 7
    }

    private static Object commonErrorHandler(ConcurrentKafkaListenerContainerFactory factory) {
        def field = ConcurrentKafkaListenerContainerFactory.class.superclass.declaredFields
                .find { it.name == 'commonErrorHandler' }
        field.accessible = true
        field.get(factory)
    }

    private static Object fieldValue(Object target, Class<?> from, String name) {
        def clazz = from
        while (clazz != null) {
            def field = clazz.declaredFields.find { it.name == name }
            if (field) {
                field.accessible = true
                return field.get(target)
            }
            clazz = clazz.superclass
        }
        throw new IllegalStateException("Field '$name' not found in $from hierarchy")
    }
}
