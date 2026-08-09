package com.hsbc.pingpong.ping.ratelimit;

import com.hsbc.pingpong.ping.config.PingProperties.RateLimit;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Same token-bucket algorithm as {@link RedisLuaRateLimiter}, but state lives in a
 * file and cross-process mutual exclusion comes from a blocking {@link FileLock}.
 * In-JVM serialization uses a static per-file monitor: java.nio throws
 * OverlappingFileLockException when two threads of the same JVM lock the same file,
 * so concurrent calls within one process must be serialized before reaching the OS lock.
 */
public class FileLockRateLimiter implements RateLimiter {

    private static final ConcurrentHashMap<String, Object> MONITORS = new ConcurrentHashMap<>();

    private final int maxTokens;
    private final int refillPerSecond;
    private final int step;
    private final LongSupplier clock;
    private final Path stateFile;

    public FileLockRateLimiter(RateLimit config) {
        this(config, System::currentTimeMillis);
    }

    FileLockRateLimiter(RateLimit config, LongSupplier clock) {
        if (config.getMaxTokens() < 1 || config.getRefillPerSecond() < 1 || config.getStep() < 1) {
            throw new IllegalArgumentException("maxTokens, refillPerSecond and step must all be >= 1");
        }
        this.maxTokens = config.getMaxTokens();
        this.refillPerSecond = config.getRefillPerSecond();
        this.step = config.getStep();
        this.clock = clock;
        this.stateFile = resolveStateFile(config).toAbsolutePath();
    }

    private static Path resolveStateFile(RateLimit config) {
        String lockFile = config.getLockFile();
        if (lockFile == null || lockFile.trim().isEmpty()) {
            lockFile = System.getProperty("java.io.tmpdir") + File.separator + config.getKey() + ".lock";
        }
        return Paths.get(lockFile);
    }

    @Override
    public boolean tryAcquire() {
        //这块加synchronized关键字是因为我之前测试的时候考虑到可能在跑pingservice的时候，定时器在调度，我也会手动调用到测试接口，所以加个了锁
        synchronized (MONITORS.computeIfAbsent(stateFile.toString(), key -> new Object())) {
            try {
                return acquireWithLock();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private boolean acquireWithLock() throws IOException {
        Path parent = stateFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (RandomAccessFile raf = new RandomAccessFile(stateFile.toFile(), "rw");
             FileChannel channel = raf.getChannel();
             FileLock ignored = channel.lock()) {
            return evaluateAndPersist(raf, clock.getAsLong());
        }
    }

    private boolean evaluateAndPersist(RandomAccessFile raf, long now) throws IOException {
        TokenBucketState state = readState(raf, now);
        state.refill(now, maxTokens, refillPerSecond);
        boolean allowed = state.currentToken >= step;
        if (allowed) {
            state.currentToken -= step;
        }
        writeState(raf, state);
        return allowed;
    }

    private TokenBucketState readState(RandomAccessFile raf, long now) throws IOException {
        if (raf.length() == 0) {
            return new TokenBucketState(now, maxTokens);
        }
        raf.seek(0);
        byte[] buf = new byte[(int) raf.length()];
        raf.readFully(buf);
        String data = new String(buf, StandardCharsets.UTF_8);
        int comma = data.indexOf(',');
        long lastTime = Long.parseLong(data.substring(0, comma).trim());
        long currentToken = Long.parseLong(data.substring(comma + 1).trim());
        return new TokenBucketState(lastTime, currentToken);
    }

    private void writeState(RandomAccessFile raf, TokenBucketState state) throws IOException {
        byte[] data = (state.lastTime + "," + state.currentToken).getBytes(StandardCharsets.UTF_8);
        raf.seek(0);
        raf.setLength(0);
        raf.write(data);
    }

    private static final class TokenBucketState {
        long lastTime;
        long currentToken;

        TokenBucketState(long lastTime, long currentToken) {
            this.lastTime = lastTime;
            this.currentToken = currentToken;
        }

        void refill(long now, int maxTokens, int refillPerSecond) {
            long elapsed = Math.max(0, now - lastTime);
            long refill = elapsed * refillPerSecond / 1000;
            if (refill > 0) {
                currentToken += refill;
                lastTime += refill * 1000L / refillPerSecond;
                if (currentToken > maxTokens) {
                    currentToken = maxTokens;
                }
            }
        }
    }
}
