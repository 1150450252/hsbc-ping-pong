package com.hsbc.pingpong.pong.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

@Component
public class TokenBucket {

    // 令牌按千分位计量，才能用整数累积不足 1 个的毫秒级增量（平滑 refill）
    private static final int TOKEN_UNIT = 1000;

    // 桶的最大容量，即突发上限（burst）
    private final int maxTokens;
    // 每秒补充的令牌数（rate）
    private final int refillPerSecond;
    private final LongSupplier clock;
    // 当前可用令牌数，单位：TOKEN_UNIT 分之一个令牌
    private final AtomicLong tokenCount;
    // 上次补充令牌的时间戳
    private final AtomicLong lastRefillMillis;

    @Autowired
    public TokenBucket(@Value("${hsbc.pingpong.max-requests-per-second:1}") int maxRequestsPerSecond,
                       @Value("${hsbc.pingpong.max-burst:0}") int maxBurst) {
        // 突发上限未配置时退化为与速率一致（等价于旧的单参数行为）
        this(maxBurst > 0 ? maxBurst : maxRequestsPerSecond, maxRequestsPerSecond, System::currentTimeMillis);
    }

    TokenBucket(int maxTokens, int refillPerSecond, LongSupplier clock) {
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be at least 1");
        }
        this.maxTokens = maxTokens;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.tokenCount = new AtomicLong((long) maxTokens * TOKEN_UNIT); // 初始满桶
        this.lastRefillMillis = new AtomicLong(clock.getAsLong());
    }

    public boolean tryAcquire() {
        refill();
        while (true) {
            long currentTokens = tokenCount.get();
            if (currentTokens < TOKEN_UNIT) {
                return false;
            }
            if (tokenCount.compareAndSet(currentTokens, currentTokens - TOKEN_UNIT)) {
                return true;
            }
        }
    }

    /**
     * 按经过的毫秒数连续补令牌：elapsed * rate / 1000 个（换算成 TOKEN_UNIT 后即 elapsed * rate）。
     * 每次调用（包括被限流的那次）都推进 lastRefillMillis，不足 1 个的增量会累积下来，不会像整秒跳跃那样丢掉。
     */
    private void refill() {
        long now = clock.getAsLong();
        long last = lastRefillMillis.get();
        if (now <= last) {
            return;
        }
        if (!lastRefillMillis.compareAndSet(last, now)) {
            return;
        }
        long capacityUnits = (long) maxTokens * TOKEN_UNIT;
        long toAdd = Math.min((now - last) * refillPerSecond, capacityUnits);
        while (true) {
            long currentTokens = tokenCount.get();
            if (currentTokens >= capacityUnits) {
                return;
            }
            long newTokens = Math.min(currentTokens + toAdd, capacityUnits);
            if (tokenCount.compareAndSet(currentTokens, newTokens)) {
                return;
            }
        }
    }
}
