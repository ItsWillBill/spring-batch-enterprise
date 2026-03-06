package com.reconciliation.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reconciliation.infrastructure.persistence.entity.LedgerEntryEntity;

/**
 * Spring Data JPA repository for {@link LedgerEntryEntity}.
 */
@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, Long> {

    Optional<LedgerEntryEntity> findByReference(String reference);
}
