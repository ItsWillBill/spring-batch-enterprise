package com.reconciliation.domain.port;

import java.util.Optional;

import com.reconciliation.domain.model.LedgerEntry;

public interface LedgerRepository {
    Optional<LedgerEntry> findByReference(String reference);
}
