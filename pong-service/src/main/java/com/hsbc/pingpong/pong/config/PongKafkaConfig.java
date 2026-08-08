package com.hsbc.pingpong.pong.config;

import com.hsbc.pingpong.common.event.PingPongEvent;
import com.hsbc.pingpong.pong.service.PingPongEventPublisher;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class PongKafkaConfig {

    @Bean
    public PingPongEventPublisher pingPongEventPublisher(KafkaTemplate<String, PingPongEvent> kafkaTemplate,
                                                         PongProperties properties,
                                                         @Value("${PING_INSTANCE_ID:pong}") String instanceId) {
        return new PingPongEventPublisher(
                kafkaTemplate,
                properties.getKafka().getTopic(),
                instanceId,
                properties.getKafka().isEnabled());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PingPongEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PingPongEvent> consumerFactory,
            KafkaTemplate<String, PingPongEvent> kafkaTemplate,
            PongProperties properties) {
        ConcurrentKafkaListenerContainerFactory<String, PingPongEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(properties.getKafka().getDltTopic(), record.partition()));
        // FixedBackOff 的 maxAttempts 含首次,所以重试次数 = maxRetries
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer,
                new FixedBackOff(properties.getKafka().getRetryBackoffMs(), properties.getKafka().getMaxRetries() + 1)));
        return factory;
    }
}
