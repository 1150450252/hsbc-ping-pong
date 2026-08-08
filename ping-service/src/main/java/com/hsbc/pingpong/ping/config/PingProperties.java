package com.hsbc.pingpong.ping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hsbc.pingpong")
public class PingProperties {

    private String instanceId = "ping";
    private String pongUrl = "http://localhost:8033";
    private RateLimit rateLimit = new RateLimit();
    private Kafka kafka = new Kafka();

    @Data
    public static class RateLimit {
        private String type = "redis-lua";
        private int maxTokens = 2;
        private int refillPerSecond = 2;
        private int step = 1;
        private String key = "ping-pong-rate-limit";
        private String lockFile;
    }

    @Data
    public static class Kafka {
        private boolean enabled = true;
        private String topic = "ping-pong-events";
    }
}
