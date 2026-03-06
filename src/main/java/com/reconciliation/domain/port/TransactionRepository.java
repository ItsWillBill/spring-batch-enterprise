package com.reconciliation.domain.port;

import java.util.Optional;

import com.reconciliation.domain.model.Transaction;

public interface TransactionRepository {

    boolean existsByExternalId(String externalId);

    Optional<Transaction> findByExternalId(String externalId);
}
