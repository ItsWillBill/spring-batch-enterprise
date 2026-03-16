package com.reconciliation.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reconciliation.infrastructure.persistence.entity.TransactionEntity;

/**
 * Spring Data JPA repository for {@link TransactionEntity}.
 *
 * <p>
 * This is purely an infrastructure concern. The domain layer never
 * interacts with this interface directly — it always goes through the
 * {@link com.reconciliation.domain.port.TransactionRepository} port.
 */

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Long> {

    boolean existsByExternalId(String externalId);

    Optional<TransactionEntity> findByExternalId(String externalId);

    List<TransactionEntity> findAllByExternalIdIn(Collection<String> externalIds);
}