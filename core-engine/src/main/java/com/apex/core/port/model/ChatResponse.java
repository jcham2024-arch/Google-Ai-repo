package com.apex.core.port.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ChatResponse {
    private final String content;
    private final String model;
    private final List<ToolCall> toolCalls;
    private final boolean success;
    private final String errorMessage;

    public ChatResponse(String content, String model, List<ToolCall> toolCalls, boolean success, String errorMessage) {
        this.content = content != null ? content : "";
        this.model = model != null ? model : "";
        this.toolCalls = toolCalls != null ? Collections.unmodifiableList(toolCalls) : Collections.emptyList();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static ChatResponse ok(String content, String model, List<ToolCall> toolCalls) {
        return new ChatResponse(content, model, toolCalls, true, null);
    }

    public static ChatResponse error(String errorMessage, String model) {
        return new ChatResponse("", model, Collections.emptyList(), false, errorMessage);
    }

    public String getContent() { return content; }
    public String getModel() { return model; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatResponse that = (ChatResponse) o;
        return success == that.success &&
               Objects.equals(content, that.content) &&
               Objects.equals(model, that.model) &&
               Objects.equals(toolCalls, that.toolCalls) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, model, toolCalls, success, errorMessage);
    }

    @Override
    public String toString() {
        return "ChatResponse{model='" + model + "', success=" + success + ", content='" + content + "'}";
    }
}
