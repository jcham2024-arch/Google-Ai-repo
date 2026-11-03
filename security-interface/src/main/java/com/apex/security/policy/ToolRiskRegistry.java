package com.apex.security.policy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToolRiskRegistry {
    private final Map<String, RiskLevel> mappings = new ConcurrentHashMap<>();

    public ToolRiskRegistry() {
        // Seed standard risk mappings
        mappings.put("read_file", RiskLevel.L0);
        mappings.put("list_files", RiskLevel.L0);
        mappings.put("write_file", RiskLevel.L1);
        mappings.put("write_file_external", RiskLevel.L2);
        mappings.put("http_get", RiskLevel.L2);
        mappings.put("http_post", RiskLevel.L3);
        mappings.put("run_command", RiskLevel.L4);
    }

    public void registerRisk(String toolName, RiskLevel level) {
        if (toolName != null && level != null) {
            mappings.put(toolName, level);
        }
    }

    public RiskLevel getRiskLevel(String toolName) {
        if (toolName == null) {
            return RiskLevel.L3;
        }
        return mappings.getOrDefault(toolName, RiskLevel.L3);
    }
}
