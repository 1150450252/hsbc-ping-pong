package com.hsbc.pingpong.ping.config

import com.hsbc.pingpong.ping.client.PingClient
import com.hsbc.pingpong.ping.event.PingEventPublisher
import com.hsbc.pingpong.ping.ratelimit.FileLockRateLimiter
import com.hsbc.pingpong.ping.ratelimit.RedisLuaRateLimiter
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

class PingConfigTest extends Specification {

    def config = new PingConfig()

    def "defines a Redis Lua rate limiter bean by default"() {
        given:
        def properties = new PingProperties()
        def redisTemplate = Mock(StringRedisTemplate)

        when:
        def limiter = config.rateLimiter(properties, redisTemplate)

        then:
        limiter instanceof RedisLuaRateLimiter
    }

    def "defines a file-lock rate limiter bean when configured"() {
        given:
        def properties = new PingProperties()
        properties.rateLimit.type = "file-lock"
        def redisTemplate = Mock(StringRedisTemplate)

        when:
        def limiter = config.rateLimiter(properties, redisTemplate)

        then:
        limiter instanceof FileLockRateLimiter
    }

    def "rejects an unknown rate limit type"() {
        given:
        def properties = new PingProperties()
        properties.rateLimit.type = "no-such-type"
        def redisTemplate = Mock(StringRedisTemplate)

        when:
        config.rateLimiter(properties, redisTemplate)

        then:
        thrown(IllegalArgumentException)
    }

    def "defines a ping client bean pointed at the pong service"() {
        given:
        def properties = new PingProperties()
        properties.setPongUrl("http://pong:8033")

        when:
        def client = config.pingClient(properties)

        then:
        client instanceof PingClient
    }

    def "defines a kafka event publisher bean from the configured topic"() {
        given:
        def properties = new PingProperties()
        def kafkaTemplate = Mock(KafkaTemplate)

        when:
        def publisher = config.pingEventPublisher(kafkaTemplate, properties)

        then:
        publisher instanceof PingEventPublisher
    }
}
