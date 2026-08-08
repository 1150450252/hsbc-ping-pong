package com.hsbc.pingpong.ping.ratelimit;

import com.hsbc.pingpong.ping.config.PingProperties.RateLimit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;


public class RedisLuaRateLimiter implements RateLimiter {

    /**
     * Boolean allowed = redisTemplate.execute(SCRIPT, keys,
     *                 String.valueOf(maxTokens),
     *                 String.valueOf(refillPerSecond),
     *                 String.valueOf(clock.getAsLong()),
     *                 String.valueOf(step));
     *         return Boolean.TRUE.equals(allowed);
     */
    static final String TOKEN_BUCKET_LUA = "" +
            // 从 Redis 哈希里读出桶的两个字段:last_time(上次补令牌时间)、current_token(当前令牌数)
            "local info = redis.call('HMGET', KEYS[1], 'last_time', 'current_token')\n" +
            // 把读到的字符串转成数字;key 不存在时这两个值都是 nil
            "local last_time = tonumber(info[1])\n" +
            "local current_token = tonumber(info[2])\n" +
            // ARGV[1] 桶的最大容量,即突发上限
            "local max_token = tonumber(ARGV[1])\n" +
            // ARGV[2] 每秒恢复的令牌数
            "local token_rate = tonumber(ARGV[2])\n" +
            // ARGV[3] 当前毫秒时间戳(由 Java 传入)
            "local current_time = tonumber(ARGV[3])\n" +
            // ARGV[4] 步长,即每次请求消耗的令牌数
            "local step = tonumber(ARGV[4])\n" +
            "\n" +
            // 首次调用:桶不存在,直接初始化为满桶,并把当前时间记为 last_time
            "if current_token == nil then\n" +
            // 满桶
            "  current_token = max_token\n" +
            // 记录首次调用的时间
            "  last_time = current_time\n" +
            // 非首次调用:按经过的时间补令牌
            "else\n" +
            // 距离上次更新过去了多少毫秒
            "  local elapsed = current_time - last_time\n" +
            // 这段时间内按速率恢复的令牌数(向下取整)
            "  local refill = math.floor(elapsed * token_rate / 1000)\n" +
            // 只有确实恢复了令牌才需要更新状态
            "  if refill > 0 then\n" +
            // 恢复的令牌入桶
            "    current_token = current_token + refill\n" +
            // 时间只推进与补入令牌对应的那部分,避免下次重复补令牌
            "    last_time = last_time + refill * 1000 / token_rate\n" +
            // 令牌数不能超过桶容量
            "    if current_token > max_token then current_token = max_token end\n" +
            "  end\n" +
            "end\n" +
            "\n" +
            // 默认结果是拒绝(0)
            "local result = 0\n" +
            // 桶内令牌够 step 个才放行
            "if current_token >= step then\n" +
            // 标记放行
            "  result = 1\n" +
            // 扣减本次消耗的令牌
            "  current_token = current_token - step\n" +
            "end\n" +
            "\n" +
            // 把更新后的时间和令牌数写回 Redis 哈希,供下一次调用读取
            "redis.call('HMSET', KEYS[1], 'last_time', last_time, 'current_token', current_token)\n" +
            // 给 key 设置过期时间:按当前剩余令牌算出重新补满所需的毫秒数 + 1000 余量,空闲的桶会自动过期清理
            "redis.call('PEXPIRE', KEYS[1], math.ceil((max_token - current_token) * 1000 / token_rate) + 1000)\n" +
            // 返回 1 放行 / 0 限流
            "return result";

    private static final RedisScript<Boolean> SCRIPT =
            new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Boolean.class);

    private final StringRedisTemplate redisTemplate;
    private final String key;
    private final int maxTokens;
    private final int refillPerSecond;
    private final int step;
    private final LongSupplier clock;

    public RedisLuaRateLimiter(StringRedisTemplate redisTemplate, RateLimit config) {
        this(redisTemplate, config, System::currentTimeMillis);
    }

    RedisLuaRateLimiter(StringRedisTemplate redisTemplate, RateLimit config, LongSupplier clock) {
        if (config.getMaxTokens() < 1 || config.getRefillPerSecond() < 1 || config.getStep() < 1) {
            throw new IllegalArgumentException("maxTokens, refillPerSecond and step must all be >= 1");
        }
        this.redisTemplate = redisTemplate;
        this.key = config.getKey();
        this.maxTokens = config.getMaxTokens();
        this.refillPerSecond = config.getRefillPerSecond();
        this.step = config.getStep();
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire() {
        List<String> keys = Collections.singletonList(key);
        Boolean allowed = redisTemplate.execute(SCRIPT, keys,
                String.valueOf(maxTokens),
                String.valueOf(refillPerSecond),
                String.valueOf(clock.getAsLong()),
                String.valueOf(step));
        return Boolean.TRUE.equals(allowed);
    }
}
