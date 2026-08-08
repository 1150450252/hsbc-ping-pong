package com.hsbc.pingpong.pong.service

import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongSupplier

class TokenBucketSpec extends Specification {

    long now = 1000L

    def "starts full and rejects requests once tokens run out"() {
        given:
        def tokenBucket = new TokenBucket(1, 1, { now } as LongSupplier)

        expect:
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == false
    }

    def "refills one token after a new second begins"() {
        given:
        def tokenBucket = new TokenBucket(1, 1, { now } as LongSupplier)
        tokenBucket.tryAcquire()
        tokenBucket.tryAcquire()

        when:
        now = 2000L

        then:
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == false
    }

    def "allows up to the bucket capacity in a burst"() {
        given:
        def tokenBucket = new TokenBucket(2, 2, { now } as LongSupplier)

        expect:
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == false
    }

    def "never refills beyond the bucket capacity after long idle"() {
        given:
        def tokenBucket = new TokenBucket(2, 2, { now } as LongSupplier)
        tokenBucket.tryAcquire()
        tokenBucket.tryAcquire()

        when:
        now = 100_000L

        then:
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == false
    }

    def "Spring-injected constructor delegates to a full bucket"() {
        given:
        def tokenBucket = new TokenBucket(2)

        expect:
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == true
        tokenBucket.tryAcquire() == false
    }

    def "rejects a maxTokens below one"() {
        when:
        new TokenBucket(0, 1, { now } as LongSupplier)

        then:
        thrown(IllegalArgumentException)
    }

    def "leaves an already-full bucket untouched when a refill is due"() {
        given:
        def tokenBucket = new TokenBucket(2, 1, { now } as LongSupplier)

        when:
        now = 2000L
        tokenBucket.tryAcquire()

        then:
        tokenBucket.tryAcquire()
        tokenBucket.tryAcquire() == false
    }

    def "stays consistent under a synchronized wave of acquisitions and refills"() {
        given:
        def clockNow = new AtomicLong(1000L)
        def threads = 16
        def rounds = 200
        def wave = new CyclicBarrier(threads)
        def tokenBucket = new TokenBucket(64, 15, { clockNow.addAndGet(1000L) } as LongSupplier)
        def acquired = new AtomicInteger()
        def denied = new AtomicInteger()
        def executor = Executors.newFixedThreadPool(threads)
        def futures = new ArrayList()

        when:
        threads.times {
            futures.add(executor.submit({
                rounds.times {
                    wave.await()
                    if (tokenBucket.tryAcquire()) {
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
