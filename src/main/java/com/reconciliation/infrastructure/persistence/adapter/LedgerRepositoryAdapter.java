package com.reconciliation.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.reconciliation.domain.model.LedgerEntry;
import com.reconciliation.domain.port.LedgerRepository;
import com.reconciliation.infrastructure.persistence.entity.LedgerEntryEntity;
import com.reconciliation.infrastructure.persistence.repository.LedgerEntryJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final LedgerEntryJpaRepository jpaRepository;

    @Override
    public Optional<LedgerEntry> findByReference(String reference) {
        return jpaRepository.findByReference(reference).map(this::toDomain);
    }

    private LedgerEntry toDomain(LedgerEntryEntity entity) {
        return new LedgerEntry(entity.getReference(), entity.getAmount(), entity.getCurrency(), entity.getEntryDate(),
                entity.getAccountCode());
    }

}
