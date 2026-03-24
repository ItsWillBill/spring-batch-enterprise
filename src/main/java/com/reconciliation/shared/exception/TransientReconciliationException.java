package com.reconciliation.shared.exception;

/**
 * Thrown when a transient error occurs during reconciliation, such as a
 * temporary network issue or a DB connectiontimeout, Redis unavailable.
 */
public class TransientReconciliationException extends ReconciliationException {

    public TransientReconciliationException(String message) {
        super(message);
    }

    public TransientReconciliationException(String message, Throwable cause) {
        super(message, cause);
    }

}
