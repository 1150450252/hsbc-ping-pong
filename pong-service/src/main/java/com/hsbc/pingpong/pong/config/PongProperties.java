package com.hsbc.pingpong.pong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hsbc.pingpong")
public class PongProperties {

    private int maxRequestsPerSecond = 1;

    private Kafka kafka = new Kafka();

    @Data
    public static class Kafka {

        private boolean enabled = true;
        private String topic = "ping-pong-events";
        private String groupId = "pong-service";
        private int maxRetries = 3;
        private String dltTopic = "ping-pong-events-dlt";
        private long retryBackoffMs = 1000;
    }
}
