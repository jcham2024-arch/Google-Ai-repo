package com.apex.core.tool;

import com.apex.core.json.MiniJson;
import java.util.Collections;
import java.util.Map;

public final class ToolDefinition {
    public enum Source { NATIVE, MCP, OPENAI, CLAUDE_SKILL, AGENTRULES, APPFUNCTIONS }
    public enum Kind   { TOOL, SKILL, DIRECTIVE }

    private final String id;
    private final String name;
    private final String description;
    private final Kind kind;
    private final Source source;
    private final MiniJson.JsonNode inputSchema;
    private final String riskLevel;
    private final TransportHint transport;
    private final Map<String, String> metadata;

    public ToolDefinition(
        String id,
        String name,
        String description,
        Kind kind,
        Source source,
        MiniJson.JsonNode inputSchema,
        String riskLevel,
        TransportHint transport,
        Map<String, String> metadata
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.kind = kind;
        this.source = source;
        this.inputSchema = inputSchema;
        this.riskLevel = riskLevel != null ? riskLevel : "L2";
        this.transport = transport != null ? transport : TransportHint.local();
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Kind getKind() { return kind; }
    public Source getSource() { return source; }
    public MiniJson.JsonNode getInputSchema() { return inputSchema; }
    public String getRiskLevel() { return riskLevel; }
    public TransportHint getTransport() { return transport; }
    public Map<String, String> getMetadata() { return metadata; }
}
