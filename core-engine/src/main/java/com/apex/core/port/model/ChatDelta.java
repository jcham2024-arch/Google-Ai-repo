package com.apex.core.port.model;

import java.util.Objects;

/**
 * Immutable incremental chat token delta emitted during SSE streaming chat requests.
 */
public final class ChatDelta {
    private final String contentDelta;
    private final String role;
    private final ToolCall toolCallDelta;
    private final boolean done;
    private final String finishReason;

    public ChatDelta(String contentDelta, String role, ToolCall toolCallDelta, boolean done, String finishReason) {
        this.contentDelta = contentDelta != null ? contentDelta : "";
        this.role = role != null ? role : "";
        this.toolCallDelta = toolCallDelta;
        this.done = done;
        this.finishReason = finishReason;
    }

    public static ChatDelta content(String delta) {
        return new ChatDelta(delta, "assistant", null, false, null);
    }

    public static ChatDelta toolCall(ToolCall toolCall) {
        return new ChatDelta("", "assistant", toolCall, false, null);
    }

    public static ChatDelta done(String finishReason) {
        return new ChatDelta("", "assistant", null, true, finishReason);
    }

    public String getContentDelta() { return contentDelta; }
    public String getRole() { return role; }
    public ToolCall getToolCallDelta() { return toolCallDelta; }
    public boolean isDone() { return done; }
    public String getFinishReason() { return finishReason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatDelta chatDelta = (ChatDelta) o;
        return done == chatDelta.done &&
               Objects.equals(contentDelta, chatDelta.contentDelta) &&
               Objects.equals(role, chatDelta.role) &&
               Objects.equals(toolCallDelta, chatDelta.toolCallDelta) &&
               Objects.equals(finishReason, chatDelta.finishReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentDelta, role, toolCallDelta, done, finishReason);
    }

    @Override
    public String toString() {
        return "ChatDelta{done=" + done + ", delta='" + contentDelta + "'}";
    }
}
