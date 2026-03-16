package com.reconciliation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(
        String externalId,
        String source,
        BigDecimal amount,
        String currency,
        LocalDate transactionDate,
        String description) {

    public Transaction {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Transaction externalId cannot be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-character ISO 4217 code");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }

        currency = currency.toUpperCase();
    }

}
