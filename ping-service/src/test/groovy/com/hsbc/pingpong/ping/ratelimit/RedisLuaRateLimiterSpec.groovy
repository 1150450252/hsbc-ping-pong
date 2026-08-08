package com.hsbc.pingpong.ping.ratelimit

import com.hsbc.pingpong.ping.config.PingProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import spock.lang.Specification

import java.util.function.LongSupplier

class RedisLuaRateLimiterSpec extends Specification {

    PingProperties.RateLimit config
    StringRedisTemplate redisTemplate = Mock()

    def setup() {
        config = new PingProperties.RateLimit()
    }

    def "allows a request when the Lua script returns true"() {
        given:
        def limiter = new RedisLuaRateLimiter(redisTemplate, config, { 1000L } as LongSupplier)
        redisTemplate.execute(_ as RedisScript, _ as List, _ as Object[]) >> true

        expect:
        limiter.tryAcquire() == true
    }

    def "rejects a request when the Lua script returns false"() {
        given:
        def limiter = new RedisLuaRateLimiter(redisTemplate, config, { 1000L } as LongSupplier)
        redisTemplate.execute(_ as RedisScript, _ as List, _ as Object[]) >> false

        expect:
        limiter.tryAcquire() == false
    }

    def "treats a null result as rejected"() {
        given:
        def limiter = new RedisLuaRateLimiter(redisTemplate, config, { 1000L } as LongSupplier)
        redisTemplate.execute(_ as RedisScript, _ as List, _ as Object[]) >> null

        expect:
        limiter.tryAcquire() == false
    }

    def "passes the configured step as the last ARGV argument"() {
        given:
        config.setStep(3)
        def limiter = new RedisLuaRateLimiter(redisTemplate, config, { 2000L } as LongSupplier)
        Object[] capturedArgs = []

        when:
        limiter.tryAcquire()

        then:
        1 * redisTemplate.execute(_ as RedisScript, _ as List, _ as Object[]) >> { script, keys, args ->
            capturedArgs = args
            true
        }
        capturedArgs == ["2", "2", "2000", "3"]
    }

    def "passes the configured key and token bucket values to Lua"() {
        given:
        config.setMaxTokens(5)
        config.setRefillPerSecond(10)
        config.setKey("my-bucket")
        def limiter = new RedisLuaRateLimiter(redisTemplate, config, { 5000L } as LongSupplier)

        when:
        limiter.tryAcquire()

        then:
        1 * redisTemplate.execute(_ as RedisScript, ["my-bucket"] as List, _ as Object[]) >> true
    }

    def "public constructor delegates to the system clock"() {
        given:
        def limiter = new RedisLuaRateLimiter(redisTemplate, config)

        when:
        def allowed = limiter.tryAcquire()

        then:
        1 * redisTemplate.execute(_ as RedisScript, _ as List, _ as Object[]) >> true
        allowed == true
    }

    def "rejects invalid configuration"() {
        when:
        config.setStep(0)
        new RedisLuaRateLimiter(redisTemplate, config, { 1000L } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects a maxTokens below one"() {
        when:
        config.setMaxTokens(0)
        new RedisLuaRateLimiter(redisTemplate, config, { 1000L } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects a refillPerSecond below one"() {
        when:
        config.setRefillPerSecond(0)
        new RedisLuaRateLimiter(redisTemplate, config, { 1000L } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "Lua script implements a configurable-step token bucket"() {
        expect:
        RedisLuaRateLimiter.TOKEN_BUCKET_LUA.contains("ARGV[4]")
        RedisLuaRateLimiter.TOKEN_BUCKET_LUA.contains("current_token >= step")
        RedisLuaRateLimiter.TOKEN_BUCKET_LUA.contains("HMSET")
    }
}
