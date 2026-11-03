package com.apex.core.tool;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Thread-safe registry and execution dispatcher for in-process {@link ToolWorker} instances.
 * <p>
 * On Android, spawning external OS subprocesses via {@code ProcessBuilder} is restricted by the
 * sandbox environment. {@code ToolRegistry} provides an in-process execution model that executes
 * tools concurrently on a managed {@link ExecutorService} thread pool while supporting cooperative
 * task cancellation via {@link Future#cancel(boolean)}.
 */
public class ToolRegistry {
    private final Map<String, ToolWorker> workers = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    /**
     * Constructs a {@code ToolRegistry} backed by a cached thread pool.
     */
    public ToolRegistry() {
        this(Executors.newCachedThreadPool());
    }

    /**
     * Constructs a {@code ToolRegistry} using the provided executor service.
     *
     * @param executorService custom thread pool executor for tool execution.
     */
    public ToolRegistry(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Registers a new tool worker. Overwrites any existing worker with the same name.
     *
     * @param worker the {@link ToolWorker} implementation to register.
     */
    public void register(ToolWorker worker) {
        if (worker != null) {
            workers.put(worker.getName(), worker);
        }
    }

    /**
     * Retrieves a registered tool worker by name.
     *
     * @param name tool identifier.
     * @return an {@link Optional} containing the worker if found, or empty if unregistered.
     */
    public Optional<ToolWorker> getWorker(String name) {
        return Optional.ofNullable(workers.get(name));
    }

    /**
     * Returns an unmodifiable snapshot view of all registered workers.
     *
     * @return read-only map of tool names to workers.
     */
    public Map<String, ToolWorker> getAllWorkers() {
        return Collections.unmodifiableMap(workers);
    }

    /**
     * Dispatches tool execution asynchronously on the managed thread pool.
     *
     * @param toolName  name of the registered tool to execute.
     * @param arguments map of string key-value arguments passed to the tool.
     * @return a {@link Future} representing the pending execution result.
     * @throws IllegalArgumentException if no tool worker is registered under {@code toolName}.
     */
    public Future<String> executeAsync(String toolName, Map<String, String> arguments) {
        ToolWorker worker = workers.get(toolName);
        if (worker == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        return executorService.submit(() -> worker.execute(arguments));
    }

    /**
     * Initiates an orderly shutdown of the underlying executor thread pool.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
