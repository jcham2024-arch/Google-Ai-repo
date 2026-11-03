package com.apex.security.policy;

/**
 * Exception thrown when tool invocation iteration limit or budget is exceeded.
 */
public class BudgetExhaustedException extends RuntimeException {
    public BudgetExhaustedException(String message) {
        super(message);
    }
}
