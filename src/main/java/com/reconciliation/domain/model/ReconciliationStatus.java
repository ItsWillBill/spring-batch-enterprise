package com.reconciliation.domain.model;

public enum ReconciliationStatus {
    /**
     * Transaction amount exactly matches the ledger entry for the same reference.
     */
    MATCHED,

    /**
     * A ledger entry was found for this reference, but the amounts differ.
     * The discrepancy amount will be recorded
     */
    DISCREPANCY,

    /**
     * No ledger entry was found for this reference.
     */
    MISSING_IN_LEDGER
}
