package com.apex.core.tool;

import java.util.Collections;
import java.util.Map;

public final class TransportHint {
    public enum Type { LOCAL, JSONRPC_HTTP, JSONRPC_SSE, HTTP_REST, MODEL_ATTACHED }

    private final Type type;
    private final String endpoint;
    private final Map<String, String> headers;

    public TransportHint(Type type, String endpoint, Map<String, String> headers) {
        this.type = type;
        this.endpoint = endpoint;
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
    }

    public static TransportHint local() {
        return new TransportHint(Type.LOCAL, null, Collections.emptyMap());
    }

    public Type getType() { return type; }
    public String getEndpoint() { return endpoint; }
    public Map<String, String> getHeaders() { return headers; }
}
