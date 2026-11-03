package com.example.load

import com.apex.core.port.model.ToolCall
import com.apex.core.tool.ToolRegistry
import com.apex.core.tool.ToolWorker
import com.apex.security.audit.AuditLogManager
import com.apex.security.interlock.SecurityInterlock
import com.apex.security.policy.SystemStateManager
import com.apex.security.policy.ToolRiskRegistry
import com.example.data.adapter.TokenBucketRateLimiter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Load smoke test asserting concurrent tool worker execution and rate limiter bounds without deadlock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LoadSmokeTest {

    @Test
    fun testConcurrentToolInvocationsWithoutDeadlock() {
        val toolRegistry = ToolRegistry()
        val riskRegistry = ToolRiskRegistry()
        val auditLogManager = AuditLogManager()

        toolRegistry.register(object : ToolWorker {
            override fun getName(): String = "read_file"
            override fun getDescription(): String = "Concurrent reader"
            override fun execute(arguments: Map<String, String>): String = "data"
        })

        val interlock = SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null)
        val completedCount = AtomicInteger(0)
        val concurrency = 20
        val latch = CountDownLatch(concurrency)

        for (i in 0 until concurrency) {
            Thread {
                try {
                    val call = ToolCall("call_$i", "read_file", emptyMap())
                    val res = interlock.execute(call)
                    if (res.isSuccess) {
                        completedCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        val pass = latch.await(10, TimeUnit.SECONDS)
        assertTrue("Load test completed within timeout without deadlock", pass)
        assertEquals(concurrency, completedCount.get())
    }

    @Test
    fun testRateLimiterThrottlingUnderSpike() {
        val rateLimiter = TokenBucketRateLimiter(maxCapacity = 5, refillTokensPerSecond = 100.0)

        // Consume all 5 permits
        assertTrue(rateLimiter.tryAcquire("test_bucket", 5, 100))

        // Immediate 6th permit acquire without timeout fails
        val acquired = rateLimiter.tryAcquire("test_bucket", 1, 10)
        assertFalse(acquired)
    }

    @Test
    fun testSystemStateTransitions() {
        val stateManager = SystemStateManager(null)
        assertEquals(SystemStateManager.State.NORMAL, stateManager.currentState)

        stateManager.transitionTo(SystemStateManager.State.DEGRADED, "Provider error rate high")
        assertEquals(SystemStateManager.State.DEGRADED, stateManager.currentState)

        stateManager.transitionTo(SystemStateManager.State.SAFE_MODE, "Audit store error")
        assertEquals(SystemStateManager.State.SAFE_MODE, stateManager.currentState)
    }
}
