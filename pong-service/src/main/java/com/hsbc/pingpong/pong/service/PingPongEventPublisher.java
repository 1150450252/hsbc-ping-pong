package com.hsbc.pingpong.pong.service;

import com.hsbc.pingpong.common.event.PingPongEvent;
import com.hsbc.pingpong.common.event.PingPongResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class PingPongEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PingPongEventPublisher.class);

    private final KafkaTemplate<String, PingPongEvent> kafkaTemplate;
    private final String topic;
    private final String instanceId;
    private final boolean enabled;

    public PingPongEventPublisher(KafkaTemplate<String, PingPongEvent> kafkaTemplate,
                                  String topic, String instanceId, boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.instanceId = instanceId;
        this.enabled = enabled;
    }

    public void publish(PingPongResult result) {
        if (!enabled) {
            return;
        }
        try {
            PingPongEvent event = new PingPongEvent(instanceId, "PONG", result, System.currentTimeMillis());
            kafkaTemplate.send(topic, event);
        } catch (Exception e) {
            log.warn("Failed to publish ping-pong event to Kafka", e);
        }
    }
}
