package com.apex.core.client;

import com.apex.core.json.MiniJson;
import com.apex.core.port.ChatModelPort;
import com.apex.core.port.HttpTransportPort;
import com.apex.core.port.JsonCodecPort;
import com.apex.core.port.model.ChatDelta;
import com.apex.core.port.model.ChatRequest;
import com.apex.core.port.model.ChatResponse;
import com.apex.core.port.model.HttpChunk;
import com.apex.core.port.model.HttpRequest;
import com.apex.core.port.model.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Universal OpenAI-compatible LLM client implementing {@link ChatModelPort} via {@link HttpTransportPort}.
 */
public class OpenAiCompatibleClient implements ChatModelPort {
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final JsonCodecPort jsonCodec;
    private final HttpTransportPort transport;

    public OpenAiCompatibleClient(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, MiniJson.INSTANCE, null);
    }

    public OpenAiCompatibleClient(String baseUrl, String apiKey, String defaultModel, JsonCodecPort jsonCodec) {
        this(baseUrl, apiKey, defaultModel, jsonCodec, null);
    }

    public OpenAiCompatibleClient(String baseUrl, String apiKey, String defaultModel,
                                  JsonCodecPort jsonCodec, HttpTransportPort transport) {
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.defaultModel = defaultModel != null ? defaultModel.trim() : "default-model";
        this.jsonCodec = jsonCodec != null ? jsonCodec : MiniJson.INSTANCE;
        this.transport = transport;
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
        String targetModel = (request.getModel() != null && !request.getModel().isEmpty()) ? request.getModel() : defaultModel;

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        if (!apiKey.isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }

        Map<String, Object> payload = new HashMap<>();
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
        String jsonPayload = jsonCodec.encodeMap(payload);

        HttpRequest httpRequest = HttpRequest.post(endpoint, headers, jsonPayload);

        if (transport == null) {
            return ChatResponse.error("No HttpTransportPort configured", targetModel);
        }

        HttpResponse response = transport.execute(httpRequest);
        if (!response.isSuccess()) {
            return ChatResponse.error("HTTP " + response.getStatusCode() + ": " + response.getCause(), targetModel);
        }

        String content = MiniJson.extractField(response.getBody(), "content");
        if (content.isEmpty()) {
            content = response.getBody();
        }

        return ChatResponse.ok(content, targetModel, Collections.emptyList());
    }

    @Override
    public void streamChat(ChatRequest request, Consumer<ChatDelta> deltaConsumer) {
        if (transport == null || deltaConsumer == null) {
            if (deltaConsumer != null) {
                deltaConsumer.accept(ChatDelta.done("no_transport"));
            }
            return;
        }

        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
        String targetModel = (request.getModel() != null && !request.getModel().isEmpty()) ? request.getModel() : defaultModel;

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "text/event-stream");
        if (!apiKey.isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", targetModel);
        payload.put("stream", true);

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
        String jsonPayload = jsonCodec.encodeMap(payload);

        HttpRequest httpRequest = HttpRequest.post(endpoint, headers, jsonPayload);

        transport.stream(httpRequest, chunk -> {
            if (chunk.isDone()) {
                deltaConsumer.accept(ChatDelta.done("stop"));
            } else if (chunk.isError()) {
                deltaConsumer.accept(ChatDelta.done("error: " + chunk.getCause()));
            } else {
                String text = chunk.getText();
                String contentDelta = MiniJson.extractField(text, "content");
                if (!contentDelta.isEmpty()) {
                    deltaConsumer.accept(ChatDelta.content(contentDelta));
                }
            }
        });
    }

    @Override
    public Iterator<String> stream(ChatRequest request) {
        ChatResponse resp = generate(request);
        if (!resp.isSuccess()) {
            return Collections.singletonList("Error: " + resp.getErrorMessage()).iterator();
        }
        return Collections.singletonList(resp.getContent()).iterator();
    }
}
