package com.apex.core.port;

import com.apex.core.port.model.ChatDelta;
import com.apex.core.port.model.ChatRequest;
import com.apex.core.port.model.ChatResponse;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Port boundary for LLM chat model generation and SSE streaming.
 */
public interface ChatModelPort {
    ChatResponse generate(ChatRequest request);

    default Iterator<String> stream(ChatRequest request) {
        ChatResponse resp = generate(request);
        if (resp != null && resp.isSuccess()) {
            return Collections.singletonList(resp.getContent()).iterator();
        }
        return Collections.emptyIterator();
    }

    default void streamChat(ChatRequest request, Consumer<ChatDelta> deltaConsumer) {
        ChatResponse resp = generate(request);
        if (resp != null && resp.isSuccess() && deltaConsumer != null) {
            deltaConsumer.accept(ChatDelta.content(resp.getContent()));
            deltaConsumer.accept(ChatDelta.done("stop"));
        } else if (deltaConsumer != null) {
            deltaConsumer.accept(ChatDelta.done("error"));
        }
    }
}
