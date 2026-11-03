package com.apex.core.tool;

import java.util.Map;

public interface ToolWorker {
    String getName();
    String getDescription();
    String execute(Map<String, String> arguments) throws Exception;
}
