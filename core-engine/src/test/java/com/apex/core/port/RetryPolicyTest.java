package com.apex.core.port;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying exponential growth, jitter limits, and max delay capping in {@link RetryPolicy}.
 */
public class RetryPolicyTest {

    @Test
    public void testMonotonicExponentialGrowthWithoutJitter() {
        // Random returning 0 jitter
        Random zeroJitterRandom = new Random() {
            @Override
            public double nextDouble() {
                return 0.5; // (0.5 * 2 - 1) = 0
            }
        };

        RetryPolicy policy = new RetryPolicy(4, 100L, 1000L, 0.2, zeroJitterRandom);

        assertEquals(0L, policy.delayForAttempt(0));
        assertEquals(100L, policy.delayForAttempt(1)); // 100 * 2^0
        assertEquals(200L, policy.delayForAttempt(2)); // 100 * 2^1
        assertEquals(400L, policy.delayForAttempt(3)); // 100 * 2^2
        assertEquals(800L, policy.delayForAttempt(4)); // 100 * 2^3
        assertEquals(1000L, policy.delayForAttempt(5)); // Capped at maxDelay 1000
    }

    @Test
    public void testJitterBounds() {
        RetryPolicy policy = new RetryPolicy(4, 500L, 8000L, 0.2);

        for (int attempt = 1; attempt <= 10; attempt++) {
            long delay = policy.delayForAttempt(attempt);
            assertTrue(delay >= 0);
            assertTrue(delay <= 8000L);
        }
    }
}
