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
import com.apex.core.port.model.ToolCall;

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
        String targetModel = request.getModel() != null && !request.getModel().isEmpty() ? request.getModel() : defaultModel;
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (!apiKey.isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", targetModel);
        payload.put("stream", false);

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

        try {
            HttpResponse response = transport.execute(httpRequest);
            if (!response.isSuccess()) {
                return ChatResponse.error("HTTP " + response.getStatusCode() + ": " + response.getBody(), targetModel);
            }

            // Parse response using jsonCodec
            Map<String, Object> responseMap = jsonCodec.decodeMap(response.getBody());
            List<Object> choices = (List<Object>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                return ChatResponse.error("No choices in response", targetModel);
            }

            Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String content = message != null ? (String) message.get("content") : "";
            if (content == null) content = "";

            // Parse tool_calls if present
            List<ToolCall> toolCalls = Collections.emptyList();
            Object toolCallsObj = message != null ? message.get("tool_calls") : null;
            if (toolCallsObj instanceof List) {
                List<?> tcList = (List<?>) toolCallsObj;
                List<ToolCall> parsed = new ArrayList<>();
                for (Object tcObj : tcList) {
                    if (tcObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tcMap = (Map<String, Object>) tcObj;
                        String id = (String) tcMap.get("id");
                        Map<String, Object> function = (Map<String, Object>) tcMap.get("function");
                        if (function != null) {
                            String name = (String) function.get("name");
                            Object argsObj = function.get("arguments");
                            Map<String, String> arguments = new HashMap<>();
                            if (argsObj instanceof String) {
                                // Parse arguments JSON string
                                Map<String, Object> argsMap = jsonCodec.decodeMap((String) argsObj);
                                for (Map.Entry<String, Object> entry : argsMap.entrySet()) {
                                    arguments.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                                }
                            } else if (argsObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> argsMap = (Map<String, Object>) argsObj;
                                for (Map.Entry<String, Object> entry : argsMap.entrySet()) {
                                    arguments.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                                }
                            }
                            parsed.add(new ToolCall(id, name, arguments));
                        }
                    }
                }
                toolCalls = parsed;
            }

            return ChatResponse.ok(content, targetModel, toolCalls);
        } catch (Exception e) {
            return ChatResponse.error("Request failed: " + e.getMessage(), targetModel);
        }
    }

    @Override
    public void streamChat(ChatRequest request, Consumer<ChatDelta> deltaConsumer) {
        String targetModel = request.getModel() != null && !request.getModel().isEmpty() ? request.getModel() : defaultModel;
        String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
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

        if (transport == null || deltaConsumer == null) {
            if (deltaConsumer != null) {
                deltaConsumer.accept(ChatDelta.done("no_transport"));
            }
            return;
        }

        transport.stream(httpRequest, chunk -> {
            if (chunk.isDone()) {
                deltaConsumer.accept(ChatDelta.done("stop"));
            } else if (chunk.isError()) {
                deltaConsumer.accept(ChatDelta.done("error: " + chunk.getCause()));
            } else {
                String text = chunk.getText();
                // Parse SSE data format: "data: {...}\n\n"
                if (text.startsWith("data: ")) {
                    String data = text.substring(6).trim();
                    if (!data.equals("[DONE]")) {
                        try {
                            Map<String, Object> chunkMap = jsonCodec.decodeMap(data);
                            List<Object> choices = (List<Object>) chunkMap.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                                if (delta != null) {
                                    String contentDelta = (String) delta.get("content");
                                    if (contentDelta != null && !contentDelta.isEmpty()) {
                                        deltaConsumer.accept(ChatDelta.content(contentDelta));
                                    }
                                    // Handle tool_calls in streaming
                                    Object toolCallsObj = delta.get("tool_calls");
                                    if (toolCallsObj instanceof List) {
                                        // For streaming, we could emit tool call deltas
                                        // For now, just log or handle as needed
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                            // Ignore parse errors in streaming chunks
                        }
                    }
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
