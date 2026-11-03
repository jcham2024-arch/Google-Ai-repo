package com.apex.core.port;

import java.util.Objects;
import java.util.Random;

/**
 * Immutable configuration and exponential backoff retry calculation policy with jitter bounds.
 */
public final class RetryPolicy {
    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final double jitterFraction;
    private final Random random;

    public RetryPolicy() {
        this(4, 500L, 8_000L, 0.2, new Random());
    }

    public RetryPolicy(int maxAttempts, long baseDelayMs, long maxDelayMs, double jitterFraction) {
        this(maxAttempts, baseDelayMs, maxDelayMs, jitterFraction, new Random());
    }

    public RetryPolicy(int maxAttempts, long baseDelayMs, long maxDelayMs, double jitterFraction, Random random) {
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 1;
        this.baseDelayMs = baseDelayMs >= 0 ? baseDelayMs : 500L;
        this.maxDelayMs = maxDelayMs >= baseDelayMs ? maxDelayMs : baseDelayMs;
        this.jitterFraction = Math.max(0.0, Math.min(1.0, jitterFraction));
        this.random = random != null ? random : new Random();
    }

    public int getMaxAttempts() { return maxAttempts; }
    public long getBaseDelayMs() { return baseDelayMs; }
    public long getMaxDelayMs() { return maxDelayMs; }
    public double getJitterFraction() { return jitterFraction; }

    public long delayForAttempt(int attempt) {
        if (attempt <= 0) return 0L;
        int expExponent = Math.min(attempt - 1, 30);
        long exp = baseDelayMs * (1L << expExponent);
        if (exp < 0 || exp > maxDelayMs) {
            exp = maxDelayMs;
        }

        double randomFactor = (random.nextDouble() * 2.0) - 1.0; // [-1.0, 1.0]
        long jitter = (long) (exp * jitterFraction * randomFactor);
        long delay = exp + jitter;

        if (delay < 0) return 0L;
        return Math.min(delay, maxDelayMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetryPolicy that = (RetryPolicy) o;
        return maxAttempts == that.maxAttempts &&
               baseDelayMs == that.baseDelayMs &&
               maxDelayMs == that.maxDelayMs &&
               Double.compare(that.jitterFraction, jitterFraction) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxAttempts, baseDelayMs, maxDelayMs, jitterFraction);
    }
}
