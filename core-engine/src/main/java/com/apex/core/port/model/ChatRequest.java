package com.apex.core.port.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ChatRequest {
    private final String prompt;
    private final String systemInstruction;
    private final String model;
    private final List<String> availableTools;

    public ChatRequest(String prompt, String systemInstruction, String model, List<String> availableTools) {
        this.prompt = prompt != null ? prompt : "";
        this.systemInstruction = systemInstruction != null ? systemInstruction : "";
        this.model = model != null ? model : "default-model";
        this.availableTools = availableTools != null ? Collections.unmodifiableList(availableTools) : Collections.emptyList();
    }

    public String getPrompt() { return prompt; }
    public String getSystemInstruction() { return systemInstruction; }
    public String getModel() { return model; }
    public List<String> getAvailableTools() { return availableTools; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRequest that = (ChatRequest) o;
        return Objects.equals(prompt, that.prompt) &&
               Objects.equals(systemInstruction, that.systemInstruction) &&
               Objects.equals(model, that.model) &&
               Objects.equals(availableTools, that.availableTools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prompt, systemInstruction, model, availableTools);
    }

    @Override
    public String toString() {
        return "ChatRequest{prompt='" + prompt + "', model='" + model + "'}";
    }
}
