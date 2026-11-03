package com.apex.core.port;

/**
 * Token-bucket rate limiter port abstraction governing inbound and outbound request throughput.
 */
public interface RateLimiterPort {
    void acquire(String bucket, int permits) throws InterruptedException;
    boolean tryAcquire(String bucket, int permits, long timeoutMs);
}
