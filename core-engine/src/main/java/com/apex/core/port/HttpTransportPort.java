package com.apex.core.port;

import com.apex.core.port.model.HttpRequest;
import com.apex.core.port.model.HttpResponse;

/**
 * Port boundary for outbound HTTP requests and streaming transport.
 */
public interface HttpTransportPort {
    /**
     * Executes a one-shot synchronous HTTP request with automatic retry handling.
     */
    HttpResponse execute(HttpRequest request);

    /**
     * Initiates a streaming HTTP request (e.g. Server-Sent Events) emitting chunks to the consumer.
     */
    void stream(HttpRequest request, ChunkConsumer consumer);
}
