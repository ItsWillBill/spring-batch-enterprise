package com.reconciliation.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.reconciliation.domain.model.LedgerEntry;
import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.Transaction;
import com.reconciliation.domain.port.LedgerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconcileTransactionUseCase {

    /**
     * Tolerance for floatting-point discrepancies in financial amounts.
     * Two amounts are considered equal if they differ by less than 0.01 (1 cent)
     */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final LedgerRepository ledgerRepository;

    public ReconciliationResult reconcile(Transaction transaction, long jobExecutionId) {
        log.debug("Reconciling transaction [exteranlId={}]", transaction.externalId());

        Optional<LedgerEntry> ledgerEntryOpt = ledgerRepository.findByReference(transaction.externalId());

        if (ledgerEntryOpt.isEmpty()) {
            log.warn("No ledger entry found for transaction [externalId={}]", transaction.externalId());
            return ReconciliationResult.missingLedgerEntry(transaction, jobExecutionId);
        }

        LedgerEntry ledgerEntry = ledgerEntryOpt.get();
        BigDecimal discrepancy = calculateDiscrepancy(transaction.amount(), ledgerEntry.amount());

        if (isWithinTolerance(discrepancy)) {
            log.debug("Transaction [externalId={}] MATCHED", transaction.externalId());
            return ReconciliationResult.matched(transaction, ledgerEntry, jobExecutionId);
        }

        log.warn("Transaction [externalId={}] has discrepancy of {}", transaction.externalId(), discrepancy);
        return ReconciliationResult.discrepancy(transaction, ledgerEntry, discrepancy, jobExecutionId);
    }

    private BigDecimal calculateDiscrepancy(BigDecimal transactionAmount, BigDecimal ledgerAmount) {
        return transactionAmount
                .subtract(ledgerAmount)
                .abs()
                .setScale(4, RoundingMode.HALF_UP);
    }

    private boolean isWithinTolerance(BigDecimal discrepancy) {
        return discrepancy.compareTo(AMOUNT_TOLERANCE) < 0;
    }
}
