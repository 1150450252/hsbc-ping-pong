package com.hsbc.pingpong.ping.config;

import com.hsbc.pingpong.common.event.PingPongEvent;
import com.hsbc.pingpong.ping.client.PingClient;
import com.hsbc.pingpong.ping.event.PingEventPublisher;
import com.hsbc.pingpong.ping.ratelimit.FileLockRateLimiter;
import com.hsbc.pingpong.ping.ratelimit.RateLimiter;
import com.hsbc.pingpong.ping.ratelimit.RedisLuaRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class PingConfig {

    @Bean
    public RateLimiter rateLimiter(PingProperties properties, StringRedisTemplate redisTemplate) {
        String type = properties.getRateLimit().getType();
        if ("file-lock".equalsIgnoreCase(type)) {
            return new FileLockRateLimiter(properties.getRateLimit());
        }
        if ("redis-lua".equalsIgnoreCase(type)) {
            return new RedisLuaRateLimiter(redisTemplate, properties.getRateLimit());
        }
        throw new IllegalArgumentException("Unknown rate-limit type: " + type + " (supported: redis-lua, file-lock)");
    }

    @Bean
    public PingClient pingClient(PingProperties properties) {
        return new PingClient(properties.getPongUrl(), properties.getInstanceId());
    }

    @Bean
    public PingEventPublisher pingEventPublisher(KafkaTemplate<String, PingPongEvent> kafkaTemplate,
                                                 PingProperties properties) {
        return new PingEventPublisher(
                kafkaTemplate,
                properties.getKafka().getTopic(),
                properties.getInstanceId(),
                properties.getKafka().isEnabled());
    }
}
