package com.reconciliation.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

public record ReconciliationResult(
        Transaction transaction,
        LedgerEntry matchedLedgerEntry,
        ReconciliationStatus status,
        BigDecimal discrepancyAmount,
        String notes,
        Long jobExecutionId) {

    public Optional<BigDecimal> getDiscrepancyAmountSafely() {
        return Optional.ofNullable(discrepancyAmount);
    }

    /**
     * Factory method to create a matched reconciliation result.
     */
    public static ReconciliationResult matched(Transaction transaction, LedgerEntry ledgerEntry, Long jobExecutionId) {
        return new ReconciliationResult(transaction, ledgerEntry, ReconciliationStatus.MATCHED, null,
                "Transaction matched ledger entry exactly", jobExecutionId);
    }

    /**
     * Factory method to create a discrepancy reconciliation result.
     */
    public static ReconciliationResult discrepancy(Transaction transaction, LedgerEntry ledgerEntry,
            BigDecimal discrepancyAmount, Long jobExecutionId) {
        String notes = String.format("Amount mismatch: transaction=%.4f, ledger=%.4f, delta=%.4f", transaction.amount(),
                ledgerEntry.amount(), discrepancyAmount);
        return new ReconciliationResult(transaction, ledgerEntry, ReconciliationStatus.DISCREPANCY, discrepancyAmount,
                notes, jobExecutionId);
    }

    /**
     * Factory method for a missing ledger entry result.
     */
    public static ReconciliationResult missingLedgerEntry(Transaction transaction, Long jobExecutionId) {
        return new ReconciliationResult(transaction, null, ReconciliationStatus.MISSING_IN_LEDGER, null,
                "No ledger entry found for reference: " + transaction.externalId(), jobExecutionId);
    }
}
