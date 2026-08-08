package com.hsbc.pingpong.pong.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

@Component
public class TokenBucket {

    // 桶的最大容量，即突发上限
    private final int maxTokens;
    // 每秒补充的令牌数
    private final int refillPerSecond;
    private final LongSupplier clock;
    // 当前可用令牌数
    private final AtomicInteger tokenCount;
    // 上次补充令牌的时间戳
    private final AtomicLong lastRefillMillis;

    @Autowired
    public TokenBucket(@Value("${hsbc.pingpong.max-requests-per-second:1}") int maxTokens) {
        //相当于每秒在令牌桶内新增了一个token
        this(maxTokens, maxTokens, System::currentTimeMillis);
    }

    TokenBucket(int maxTokens, int refillPerSecond, LongSupplier clock) {
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be at least 1");
        }
        this.maxTokens = maxTokens;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.tokenCount = new AtomicInteger(maxTokens); // 初始满桶
        this.lastRefillMillis = new AtomicLong(clock.getAsLong());
    }

    /**
     * 尝试获取一个令牌。
     *
     * @return true 表示获取成功，false 表示令牌不足被限流
     */
    public boolean tryAcquire() {
        refill();
        while (true) {
            int currentTokens = tokenCount.get();
            if (currentTokens == 0) {
                return false;
            }
            if (tokenCount.compareAndSet(currentTokens, currentTokens - 1)) {
                return true;
            }
        }
    }

    /**
     * 超过一秒就补回对应秒数 * refillPerSecond 的令牌，最多补到 maxTokens。
     */
    private void refill() {
        long now = clock.getAsLong();
        long last = lastRefillMillis.get();
        if (now - last < 1000L) {
            return;
        }
        if (!lastRefillMillis.compareAndSet(last, now)) {
            return; 
        }
        int toAdd = (int) Math.min(((now - last) / 1000L) * refillPerSecond, maxTokens);
        while (true) {
            int currentTokens = tokenCount.get();
            if (currentTokens >= maxTokens) {
                return;
            }
            int newTokens = Math.min(currentTokens + toAdd, maxTokens);
            if (tokenCount.compareAndSet(currentTokens, newTokens)) {
                return;
            }
        }
    }
}
