package com.hsbc.pingpong.ping.ratelimit;

public interface RateLimiter {

    boolean tryAcquire();
}
