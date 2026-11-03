package com.example.e2e

import com.apex.core.client.OpenAiCompatibleClient
import com.apex.core.port.ApprovalPort
import com.apex.core.port.ChunkConsumer
import com.apex.core.port.RetryPolicy
import com.apex.core.port.model.ApprovalDecision
import com.apex.core.port.model.ChatDelta
import com.apex.core.port.model.ChatRequest
import com.apex.core.port.model.ChatResponse
import com.apex.core.port.model.HttpChunk
import com.apex.core.port.model.ToolCall
import com.apex.core.tool.ToolRegistry
import com.apex.core.tool.ToolWorker
import com.apex.security.audit.AuditLogManager
import com.apex.security.audit.FileAuditStore
import com.apex.security.interlock.SecurityInterlock
import com.apex.security.interlock.ToolInvocationInterceptor
import com.apex.security.policy.RiskLevel
import com.apex.security.policy.ToolRiskRegistry
import com.example.data.adapter.OkHttpTransport
import com.example.data.adapter.OrgJsonCodec
import com.example.data.adapter.OtelTelemetry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * End-to-end integration test suite validating the complete agent pipeline:
 * :app -> :security-interface -> :core-engine using MockWebServer and frozen fixtures.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgentPipelineE2ETest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tempAuditDir: File
    private lateinit var fileAuditStore: FileAuditStore
    private lateinit var auditLogManager: AuditLogManager
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var riskRegistry: ToolRiskRegistry
    private lateinit var telemetry: OtelTelemetry

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tempAuditDir = File(System.getProperty("java.io.tmpdir"), "apex_e2e_audit_" + System.nanoTime())
        tempAuditDir.mkdirs()

        fileAuditStore = FileAuditStore(tempAuditDir)
        auditLogManager = AuditLogManager(fileAuditStore)
        toolRegistry = ToolRegistry()
        riskRegistry = ToolRiskRegistry()
        telemetry = OtelTelemetry()

        // Register sample tool
        toolRegistry.register(object : ToolWorker {
            override fun getName(): String = "read_file"
            override fun getDescription(): String = "Reads file"
            override fun execute(arguments: Map<String, String>): String = "file_contents_of_${arguments["path"]}"
        })

        toolRegistry.register(object : ToolWorker {
            override fun getName(): String = "run_command"
            override fun getDescription(): String = "Executes command"
            override fun execute(arguments: Map<String, String>): String = "executed_${arguments["cmd"]}"
        })
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        fileAuditStore.close()
        tempAuditDir.deleteRecursively()
    }

    private fun readFixture(fileName: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("llm-fixtures/$fileName")
            ?: File("src/test/resources/llm-fixtures/$fileName").inputStream()
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun `simple reply is returned and audited`() {
        val jsonFixture = readFixture("simple-reply.json")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonFixture))

        val transport = OkHttpTransport()
        val client = OpenAiCompatibleClient(mockWebServer.url("/v1").toString(), "test-key", "gpt-4o", OrgJsonCodec.INSTANCE, transport)

        val request = ChatRequest("Hello", "Be helpful", "gpt-4o", emptyList())
        val response: ChatResponse = client.generate(request)

        assertTrue(response.isSuccess)
        assertTrue(response.content.contains("Hello! How can I assist you today?"))
    }

    @Test
    fun `tool call flows through interlock and executes with audit and telemetry`() {
        val interlock = SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null)
        val interceptor = ToolInvocationInterceptor(interlock, telemetry)

        val toolCall = ToolCall("call_1", "read_file", mapOf("path" to "README.md"))
        val result = interceptor.execute(toolCall)

        assertTrue(result.isSuccess)
        assertEquals("file_contents_of_README.md", result.output)

        // Verify audit chain
        assertNull(fileAuditStore.verify())
        val trail = auditLogManager.getTrail()
        assertEquals(1, trail.size)
        assertEquals("read_file", trail[0].toolName)
        assertEquals("EXECUTED_OK", trail[0].decision)
    }

    @Test
    fun `L3 tool without approval is denied and audit records the denial`() {
        val interlock = SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null)
        val interceptor = ToolInvocationInterceptor(interlock, telemetry)

        val toolCall = ToolCall("call_2", "run_command", mapOf("cmd" to "ls -la"))
        val result = interceptor.execute(toolCall)

        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage.contains("no ApprovalPort configured"))

        assertNull(fileAuditStore.verify())
        val trail = auditLogManager.getTrail()
        assertEquals(1, trail.size)
        assertEquals("run_command", trail[0].toolName)
        assertEquals("DENIED_NO_APPROVER", trail[0].decision)
    }

    @Test
    fun `rate limited request retries and succeeds`() {
        val rateLimitResp = MockResponse().setResponseCode(429).setHeader("Retry-After", "1").setBody("Rate limit")
        val successResp = MockResponse().setResponseCode(200).setBody(readFixture("rate-limited-then-ok.json"))

        mockWebServer.enqueue(rateLimitResp)
        mockWebServer.enqueue(successResp)

        val retryPolicy = RetryPolicy(3, 10L, 100L, 0.0)
        val transport = OkHttpTransport(retryPolicy = retryPolicy)
        val client = OpenAiCompatibleClient(mockWebServer.url("/v1").toString(), "test-key", "gpt-4o", OrgJsonCodec.INSTANCE, transport)

        val response = client.generate(ChatRequest("Test retry", "", "gpt-4o", emptyList()))

        assertTrue(response.isSuccess)
        assertTrue(response.content.contains("succeeded after rate limit retry"))
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `streamed reply is delivered incrementally`() {
        val sseFixture = readFixture("streamed-reply.sse")
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "text/event-stream").setBody(sseFixture))

        val transport = OkHttpTransport()
        val client = OpenAiCompatibleClient(mockWebServer.url("/v1").toString(), "test-key", "gpt-4o", OrgJsonCodec.INSTANCE, transport)

        val receivedDeltas = mutableListOf<String>()
        val isDone = AtomicBoolean(false)

        client.streamChat(ChatRequest("Stream prompt", "", "gpt-4o", emptyList())) { delta ->
            if (delta.isDone) {
                isDone.set(true)
            } else if (delta.contentDelta.isNotEmpty()) {
                receivedDeltas.add(delta.contentDelta)
            }
        }

        assertTrue(isDone.get())
        assertTrue(receivedDeltas.size >= 1)
    }
}
