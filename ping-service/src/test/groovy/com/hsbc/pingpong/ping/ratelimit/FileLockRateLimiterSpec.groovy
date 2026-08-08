package com.hsbc.pingpong.ping.ratelimit

import com.hsbc.pingpong.ping.config.PingProperties
import spock.lang.Specification

import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongSupplier

class FileLockRateLimiterSpec extends Specification {

    long now = 1000L
    File tempFile

    def setup() {
        tempFile = File.createTempFile("pingpong-ratelimit", ".lock")
        tempFile.deleteOnExit()
    }

    private PingProperties.RateLimit config(int maxTokens, int refillPerSecond, int step) {
        def config = new PingProperties.RateLimit()
        config.maxTokens = maxTokens
        config.refillPerSecond = refillPerSecond
        config.step = step
        config.lockFile = tempFile.absolutePath
        return config
    }

    def "starts full and rejects requests once tokens run out"() {
        given:
        def limiter = new FileLockRateLimiter(config(1, 1, 1), { now } as LongSupplier)

        expect:
        limiter.tryAcquire() == true
        limiter.tryAcquire() == false
    }

    def "refills one token after a new second begins"() {
        given:
        def limiter = new FileLockRateLimiter(config(1, 1, 1), { now } as LongSupplier)
        limiter.tryAcquire()
        limiter.tryAcquire()

        when:
        now = 2000L

        then:
        limiter.tryAcquire() == true
        limiter.tryAcquire() == false
    }

    def "allows up to the bucket capacity in a burst"() {
        given:
        def limiter = new FileLockRateLimiter(config(2, 2, 1), { now } as LongSupplier)

        expect:
        limiter.tryAcquire() == true
        limiter.tryAcquire() == true
        limiter.tryAcquire() == false
    }

    def "never refills beyond the bucket capacity after long idle"() {
        given:
        def limiter = new FileLockRateLimiter(config(2, 2, 1), { now } as LongSupplier)
        limiter.tryAcquire()
        limiter.tryAcquire()

        when:
        now = 100_000L

        then:
        limiter.tryAcquire() == true
        limiter.tryAcquire() == true
        limiter.tryAcquire() == false
    }

    def "rejects a maxTokens below one"() {
        when:
        new FileLockRateLimiter(config(0, 1, 1), { now } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects a refillPerSecond below one"() {
        when:
        new FileLockRateLimiter(config(1, 0, 1), { now } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects a step below one"() {
        when:
        new FileLockRateLimiter(config(1, 1, 0), { now } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "public constructor delegates to the system clock"() {
        given:
        def limiter = new FileLockRateLimiter(config(1, 1, 1))

        when:
        def allowed = limiter.tryAcquire()

        then:
        allowed == true
    }

    def "fails fast with UncheckedIOException when the lock file cannot be created"() {
        given:
        def blocker = File.createTempFile("pingpong-blocker", ".file")
        blocker.deleteOnExit()
        def config = new PingProperties.RateLimit()
        config.maxTokens = 1
        config.refillPerSecond = 1
        config.step = 1
        config.lockFile = new File(blocker, "sub/state.lock").path
        def limiter = new FileLockRateLimiter(config, { now } as LongSupplier)

        when:
        limiter.tryAcquire()

        then:
        thrown(UncheckedIOException)
    }

    def "skips parent creation when the lock file path has no parent"() {
        given:
        def config = new PingProperties.RateLimit()
        config.maxTokens = 1
        config.refillPerSecond = 1
        config.step = 1
        config.lockFile = File.separator
        def limiter = new FileLockRateLimiter(config, { now } as LongSupplier)

        when:
        limiter.tryAcquire()

        then:
        thrown(UncheckedIOException)
    }

    def "two instances sharing the same file observe the same bucket"() {
        given:
        def limiter1 = new FileLockRateLimiter(config(1, 1, 1), { now } as LongSupplier)
        def limiter2 = new FileLockRateLimiter(config(1, 1, 1), { now } as LongSupplier)

        expect:
        limiter1.tryAcquire() == true
        limiter2.tryAcquire() == false

        when:
        now = 2000L

        then:
        limiter2.tryAcquire() == true
        limiter1.tryAcquire() == false
    }

    def "defaults the state file to tmpdir when no lock file is configured"() {
        given:
        def key = "rate-limit-" + UUID.randomUUID()
        def config = new PingProperties.RateLimit()
        config.maxTokens = 1
        config.refillPerSecond = 1
        config.step = 1
        config.key = key
        def limiter = new FileLockRateLimiter(config, { now } as LongSupplier)
        def stateFile = new File(System.getProperty("java.io.tmpdir") + File.separator + key + ".lock")

        when:
        def allowed = limiter.tryAcquire()

        then:
        allowed == true
        stateFile.exists()

        cleanup:
        stateFile.delete()
    }

    def "defaults to tmpdir when the lock file is blank"() {
        given:
        def key = "rate-limit-" + UUID.randomUUID()
        def config = new PingProperties.RateLimit()
        config.maxTokens = 1
        config.refillPerSecond = 1
        config.step = 1
        config.key = key
        config.lockFile = "   "
        def limiter = new FileLockRateLimiter(config, { now } as LongSupplier)
        def stateFile = new File(System.getProperty("java.io.tmpdir") + File.separator + key + ".lock")

        when:
        def allowed = limiter.tryAcquire()

        then:
        allowed == true
        stateFile.exists()

        cleanup:
        stateFile.delete()
    }

    def "stays consistent when two instances share the file under concurrent load"() {
        given:
        def clockNow = new AtomicLong(1000L)
        def threads = 8
        def rounds = 100
        def wave = new CyclicBarrier(threads)
        def limiter1 = new FileLockRateLimiter(config(64, 15, 1), { clockNow.addAndGet(1000L) } as LongSupplier)
        def limiter2 = new FileLockRateLimiter(config(64, 15, 1), { clockNow.addAndGet(1000L) } as LongSupplier)
        def acquired = new AtomicInteger()
        def denied = new AtomicInteger()
        def executor = Executors.newFixedThreadPool(threads)
        def futures = new ArrayList()

        when:
        threads.times {
            def limiter = it % 2 == 0 ? limiter1 : limiter2
            futures.add(executor.submit({
                rounds.times {
                    wave.await()
                    if (limiter.tryAcquire()) {
                        acquired.incrementAndGet()
                    } else {
                        denied.incrementAndGet()
                    }
                    wave.await()
                }
                return null
            } as Callable))
        }
        futures.each { it.get() }
        executor.shutdown()

        then:
        acquired.get() + denied.get() == threads * rounds
    }
}
