package com.reconciliation.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_results")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    /**
     * The DB ID of the matched ledger entry.
     * Stored as a plain Long (not a @ManyToOne) because ledger entries
     * may originate from a separate microservice in future architecture.
     */
    @Column(name = "ledger_entry_id")
    private Long ledgerEntryId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "discrepancy_amount", precision = 19, scale = 4)
    private BigDecimal discrepancyAmount;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "job_execution_id", nullable = false)
    private Long jobExecutionId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }
}