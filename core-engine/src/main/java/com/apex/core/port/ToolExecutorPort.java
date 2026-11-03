package com.apex.core.port;

import com.apex.core.port.model.ToolCall;
import com.apex.core.port.model.ExecutionResult;

public interface ToolExecutorPort {
    ExecutionResult execute(ToolCall toolCall);
}
