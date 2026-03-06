package com.reconciliation.batch.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.ReconciliationStatus;
import com.reconciliation.domain.model.Transaction;
import com.reconciliation.domain.model.TransactionProcessingStatus;
import com.reconciliation.infrastructure.persistence.entity.ReconciliationResultEntity;
import com.reconciliation.infrastructure.persistence.entity.TransactionEntity;
import com.reconciliation.infrastructure.persistence.repository.ReconciliationResultJpaRepository;
import com.reconciliation.infrastructure.persistence.repository.TransactionJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReconciliationResultItemWriter implements ItemWriter<ReconciliationResult> {

        private final TransactionJpaRepository transactionJpaRepository;
        private final ReconciliationResultJpaRepository reconciliationResultJpaRepository;

        @Override
        public void write(Chunk<? extends ReconciliationResult> chunk) throws Exception {
                List<? extends ReconciliationResult> items = chunk.getItems();
                log.debug("Writing chunk of {} reconciliation results", chunk.size());

                Set<String> externalIds = items.stream()
                                .map(r -> r.transaction().externalId())
                                .collect(Collectors.toSet());

                Map<String, TransactionEntity> existingByExternalId = transactionJpaRepository
                                .findAllByExternalIdIn(externalIds)
                                .stream()
                                .collect(Collectors.toMap(TransactionEntity::getExternalId, Function.identity()));

                List<TransactionEntity> newEntities = items.stream()
                                .map(ReconciliationResult::transaction)
                                .filter(txn -> !existingByExternalId.containsKey(txn.externalId()))
                                .collect(Collectors.toMap(txn -> txn.externalId(), this::toTransactionEntity,
                                                (a, b) -> a))
                                .values()
                                .stream()
                                .toList();

                if (!newEntities.isEmpty()) {
                        persistNewTransactions(newEntities, existingByExternalId);
                }

                List<ReconciliationResultEntity> resultEntities = items.stream()
                                .map(result -> {
                                        TransactionEntity txnEntity = existingByExternalId
                                                        .get(result.transaction().externalId());
                                        if (txnEntity == null) {
                                                throw new IllegalStateException(
                                                                "TransactionEntity missing from map after persistence step - externalId="
                                                                                + result.transaction().externalId()
                                                                                + ". This indicates a logic error in the writer implementation.");
                                        }
                                        return toResultEntity(result, txnEntity);
                                })
                                .collect(Collectors.toCollection(() -> new ArrayList<>(items.size())));

                reconciliationResultJpaRepository.saveAll(resultEntities);
                logChunkSummary(items);
        }

        private void persistNewTransactions(List<TransactionEntity> newEntities,
                        Map<String, TransactionEntity> existingByExternalId) {
                try {
                        List<TransactionEntity> savedEntities = transactionJpaRepository.saveAll(newEntities);
                        savedEntities.forEach(e -> existingByExternalId.put(e.getExternalId(), e));
                        log.debug("Bulk-inserted {} new transaction entities", savedEntities.size());
                } catch (DataIntegrityViolationException e) {
                        log.warn("Duplicate key detected during transaction insert - likely a concurrent writer. " +
                                        "Re-querying committed rows. Count={}", newEntities.size());

                        Set<String> conflictedIds = newEntities.stream()
                                        .map(TransactionEntity::getExternalId)
                                        .collect(Collectors.toSet());
                        transactionJpaRepository.findAllByExternalIdIn(conflictedIds)
                                        .forEach(entity -> existingByExternalId.put(entity.getExternalId(), entity));
                }
        }

        private TransactionEntity toTransactionEntity(Transaction transaction) {
                var entity = new TransactionEntity();
                entity.setExternalId(transaction.externalId());
                entity.setSource(transaction.source());
                entity.setAmount(transaction.amount());
                entity.setCurrency(transaction.currency());
                entity.setTransactionDate(transaction.transactionDate());
                entity.setDescription(transaction.description());
                entity.setProcessingStatus(TransactionProcessingStatus.PENDING);
                return entity;
        }

        private ReconciliationResultEntity toResultEntity(ReconciliationResult result,
                        TransactionEntity transactionEntity) {
                var entity = new ReconciliationResultEntity();
                entity.setTransaction(transactionEntity);
                entity.setStatus(result.status().name());
                entity.setDiscrepancyAmount(result.discrepancyAmount());
                entity.setNotes(result.notes());
                entity.setJobExecutionId(result.jobExecutionId());
                return entity;
        }

        private void logChunkSummary(List<? extends ReconciliationResult> items) {
                Map<ReconciliationStatus, Long> counts = items.stream()
                                .collect(Collectors.groupingBy(ReconciliationResult::status, Collectors.counting()));

                log.info("Chunk written: total={}, MATCHED={}, DISCREPANCY={}, MISSING_IN_LEDGER={}",
                                items.size(),
                                counts.getOrDefault(ReconciliationStatus.MATCHED, 0L),
                                counts.getOrDefault(ReconciliationStatus.DISCREPANCY, 0L),
                                counts.getOrDefault(ReconciliationStatus.MISSING_IN_LEDGER, 0L));
        }

}
