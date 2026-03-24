package com.reconciliation.batch.processor;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.reconciliation.application.usecase.ReconcileTransactionUseCase;
import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.Transaction;
import com.reconciliation.infrastructure.redis.DeduplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationItemProcessor implements ItemProcessor<Transaction, ReconciliationResult> {

    private final ReconcileTransactionUseCase reconcileUseCase;
    private final DeduplicationService deduplicationService;

    private Long jobExecutionId;
    private String runDate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.jobExecutionId = stepExecution.getJobExecutionId();
        this.runDate = stepExecution.getJobParameters().getString("runDate");
        log.info("Processor initialized - jobExecutionId={}, runDate={}", jobExecutionId, runDate);
    }

    @Override
    public ReconciliationResult process(@NonNull Transaction item) throws Exception {
        String externalId = item.externalId();

        // -- Deduplication check --
        if (deduplicationService.isDuplicate(externalId, runDate)) {
            log.debug("Filtering duplicate transaction [externalId={}] for runDate={}", externalId, runDate);
            return null; // Skip duplicate transactions
        }

        // -- Reconciliation --
        ReconciliationResult result = reconcileUseCase.reconcile(item, jobExecutionId);

        deduplicationService.markProcessed(externalId, runDate); // Mark as processed for deduplication

        return result;
    }

}
