package com.reconciliation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LedgerEntry(
        String reference,
        BigDecimal amount,
        String currency,
        LocalDate entryDate,
        String accountCode) {

    public LedgerEntry {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Ledger entry reference must not be blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Ledger entry amount must not be null");
        }

        currency = currency != null ? currency.toUpperCase() : "EUR";
    }

}
