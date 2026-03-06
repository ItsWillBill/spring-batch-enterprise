package com.reconciliation.domain.model;

/**
 * Enum representing the processing status of a transaction during
 * reconciliation.
 */

public enum TransactionProcessingStatus {
    /**
     * Transaction has been ingested but not yet processed by the reconciliation
     * job.
     */
    PENDING,

    /**
     * Transaction was successfully processed. The outcome
     * (matched/discrepancy/missing
     * is recorded in the associated {@code reconcilation_result} row.
     */
    PROCESSED,

    /**
     * Transaction was skipped during processing due to an error (e.g. invalid
     * data).
     */
    SKIPPED,
}
