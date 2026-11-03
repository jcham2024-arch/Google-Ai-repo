package com.apex.core.port;

import java.util.List;

public interface VectorStorePort {
    void store(String id, float[] embedding, String content);
    List<String> searchNearest(float[] queryEmbedding, int topK);
}
