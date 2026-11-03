package com.apex.core.client;

import com.apex.core.json.MiniJson;
import com.apex.core.port.ChatModelPort;
import com.apex.core.port.model.ChatRequest;
import com.apex.core.port.model.ChatResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Universal OpenAI-compatible LLM HTTP client implementing {@link ChatModelPort}.
 * <p>
 * Architectural Constraints:
 * <ul>
 *   <li>Runs in pure Java 11 with zero external libraries (no OkHttp, Retrofit, or Gson).</li>
 *   <li>Communicates via standard {@link HttpURLConnection}.</li>
 *   <li>Serializes and parses JSON payloads using hand-rolled {@link MiniJson}.</li>
 *   <li>Supports arbitrary OpenAI-compatible endpoints (OpenAI, OpenRouter, Groq, DeepSeek,
 *       Gemini OpenAI-compatible gateway, NVIDIA NIM, and local Ollama/LM Studio).</li>
 * </ul>
 */
public class OpenAiCompatibleClient implements ChatModelPort {
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;

    /**
     * Constructs a client configured for a specific provider endpoint.
     *
     * @param baseUrl      target API root URL (e.g. {@code "https://openrouter.ai/api/v1"}).
     * @param apiKey       bearer authentication token, or blank if unauthenticated/local.
     * @param defaultModel fallback model name if unspecified in requests.
     */
    public OpenAiCompatibleClient(String baseUrl, String apiKey, String defaultModel) {
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.defaultModel = defaultModel != null ? defaultModel.trim() : "default-model";
    }

    /**
     * Executes a synchronous completion turn against the target OpenAI-compatible endpoint.
     *
     * @param request the chat parameters including prompt, system prompt, and model.
     * @return a {@link ChatResponse} containing generated content or error details.
     */
    @Override
    public ChatResponse generate(ChatRequest request) {
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (!apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            Map<String, Object> payload = new HashMap<>();
            String targetModel = (request.getModel() != null && !request.getModel().isEmpty()) ? request.getModel() : defaultModel;
            payload.put("model", targetModel);

            List<Map<String, Object>> messages = new ArrayList<>();
            if (request.getSystemInstruction() != null && !request.getSystemInstruction().isEmpty()) {
                Map<String, Object> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", request.getSystemInstruction());
                messages.add(sysMsg);
            }
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getPrompt());
            messages.add(userMsg);

            payload.put("messages", messages);
            String jsonPayload = MiniJson.toJson(payload);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder responseText = new StringBuilder();
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line);
                    }
                }
            }

            if (code < 200 || code >= 300) {
                return ChatResponse.error("HTTP " + code + ": " + responseText, targetModel);
            }

            String content = MiniJson.extractField(responseText.toString(), "content");
            if (content.isEmpty()) {
                content = responseText.toString();
            }

            return ChatResponse.ok(content, targetModel, Collections.emptyList());

        } catch (Exception e) {
            return ChatResponse.error("Connection failed: " + e.getMessage(), request.getModel());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Streams completion chunks as a string iterator.
     *
     * @param request the chat prompt and model instructions.
     * @return iterator yielding completion text increments.
     */
    @Override
    public Iterator<String> stream(ChatRequest request) {
        ChatResponse resp = generate(request);
        if (!resp.isSuccess()) {
            return Collections.singletonList("Error: " + resp.getErrorMessage()).iterator();
        }
        return Collections.singletonList(resp.getContent()).iterator();
    }
}
