package com.reconciliation.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reconciliation.infrastructure.persistence.entity.ReconciliationResultEntity;

@Repository
public interface ReconciliationResultJpaRepository extends JpaRepository<ReconciliationResultEntity, Long> {
}