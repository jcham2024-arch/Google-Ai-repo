package com.apex.core.tool;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ToolRegistry {
    private final Map<String, ToolWorker> workers = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public ToolRegistry() {
        this(Executors.newCachedThreadPool());
    }

    public ToolRegistry(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void register(ToolWorker worker) {
        if (worker != null) {
            workers.put(worker.getName(), worker);
        }
    }

    public Optional<ToolWorker> getWorker(String name) {
        return Optional.ofNullable(workers.get(name));
    }

    public Map<String, ToolWorker> getAllWorkers() {
        return Collections.unmodifiableMap(workers);
    }

    public Future<String> executeAsync(String toolName, Map<String, String> arguments) {
        ToolWorker worker = workers.get(toolName);
        if (worker == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        return executorService.submit(() -> worker.execute(arguments));
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
