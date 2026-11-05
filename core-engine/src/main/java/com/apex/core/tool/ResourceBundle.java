package com.apex.core.tool;

import java.util.Collections;
import java.util.List;

public final class ResourceBundle {
    private final List<ToolDefinition> tools;
    private final List<ToolDefinition> skills;
    private final List<ToolDefinition> directives;

    public ResourceBundle(
        List<ToolDefinition> tools,
        List<ToolDefinition> skills,
        List<ToolDefinition> directives
    ) {
        this.tools = tools != null ? Collections.unmodifiableList(tools) : Collections.emptyList();
        this.skills = skills != null ? Collections.unmodifiableList(skills) : Collections.emptyList();
        this.directives = directives != null ? Collections.unmodifiableList(directives) : Collections.emptyList();
    }

    public List<ToolDefinition> getTools() { return tools; }
    public List<ToolDefinition> getSkills() { return skills; }
    public List<ToolDefinition> getDirectives() { return directives; }
}
