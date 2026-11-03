package com.apex.core.port;

import com.apex.core.port.model.HttpChunk;

/**
 * Functional callback consumer for streaming HTTP chunks in pure Java 11.
 */
@FunctionalInterface
public interface ChunkConsumer {
    void accept(HttpChunk chunk);
}
