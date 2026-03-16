package com.reconciliation.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.reconciliation.domain.model.Transaction;
import com.reconciliation.domain.port.TransactionRepository;
import com.reconciliation.infrastructure.persistence.entity.TransactionEntity;
import com.reconciliation.infrastructure.persistence.repository.TransactionJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    @Override
    public boolean existsByExternalId(String externalId) {
        return jpaRepository.existsByExternalId(externalId);
    }

    @Override
    public Optional<Transaction> findByExternalId(String externalId) {
        return jpaRepository.findByExternalId(externalId).map(this::toDomain);
    }

    private Transaction toDomain(TransactionEntity entity) {
        return new Transaction(entity.getExternalId(), entity.getSource(), entity.getAmount(), entity.getCurrency(),
                entity.getTransactionDate(), entity.getDescription());
    }

}
