package com.example.data.adapter

import com.apex.core.port.RateLimiterPort
import com.apex.core.port.SafeTelemetryDecorator
import com.apex.core.port.TelemetryPort
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe token-bucket rate limiter implementing [RateLimiterPort] in `:app`.
 */
class TokenBucketRateLimiter(
    private val maxCapacity: Int = 60,
    private val refillTokensPerSecond: Double = 10.0,
    telemetry: TelemetryPort? = null
) : RateLimiterPort {

    private val safeTelemetry = SafeTelemetryDecorator(telemetry)
    private val buckets = ConcurrentHashMap<String, BucketState>()

    private class BucketState(
        val capacity: Int,
        val refillRate: Double
    ) {
        var tokens: Double = capacity.toDouble()
        var lastRefillMs: Long = System.currentTimeMillis()

        @Synchronized
        fun refill() {
            val now = System.currentTimeMillis()
            val elapsedSec = (now - lastRefillMs) / 1000.0
            if (elapsedSec > 0) {
                tokens = Math.min(capacity.toDouble(), tokens + elapsedSec * refillRate)
                lastRefillMs = now
            }
        }

        @Synchronized
        fun tryAcquire(permits: Int): Boolean {
            refill()
            if (tokens >= permits) {
                tokens -= permits
                return true
            }
            return false
        }
    }

    override fun acquire(bucket: String, permits: Int) {
        val state = buckets.computeIfAbsent(bucket) { BucketState(maxCapacity, refillTokensPerSecond) }
        val start = System.currentTimeMillis()
        while (!state.tryAcquire(permits)) {
            safeTelemetry.counter("ratelimit.throttled", 1, mapOf("bucket" to bucket))
            Thread.sleep(50)
            if (System.currentTimeMillis() - start > 30_000) {
                safeTelemetry.counter("ratelimit.timeout", 1, mapOf("bucket" to bucket))
                throw IllegalStateException("Rate limit acquire timeout exceeded for bucket: $bucket")
            }
        }
        safeTelemetry.counter("ratelimit.acquired", 1, mapOf("bucket" to bucket))
    }

    override fun tryAcquire(bucket: String, permits: Int, timeoutMs: Long): Boolean {
        val state = buckets.computeIfAbsent(bucket) { BucketState(maxCapacity, refillTokensPerSecond) }
        val start = System.currentTimeMillis()
        while (!state.tryAcquire(permits)) {
            safeTelemetry.counter("ratelimit.throttled", 1, mapOf("bucket" to bucket))
            val elapsed = System.currentTimeMillis() - start
            if (elapsed >= timeoutMs) {
                safeTelemetry.counter("ratelimit.timeout", 1, mapOf("bucket" to bucket))
                return false
            }
            Thread.sleep(Math.min(50L, timeoutMs - elapsed))
        }
        safeTelemetry.counter("ratelimit.acquired", 1, mapOf("bucket" to bucket))
        return true
    }
}
