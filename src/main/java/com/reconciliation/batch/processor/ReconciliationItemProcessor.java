package com.reconciliation.batch.processor;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.reconciliation.application.usecase.ReconcileTransactionUseCase;
import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.Transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationItemProcessor implements ItemProcessor<Transaction, ReconciliationResult> {

    private final ReconcileTransactionUseCase reconcileUseCase;
    private Long jobExecutionId;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.jobExecutionId = stepExecution.getJobExecutionId();
        log.info("Processor initialized for jobExecutionId={}", jobExecutionId);
    }

    @Override
    public ReconciliationResult process(@NonNull Transaction item) throws Exception {
        log.debug("Processing transaction [externalId={}]", item.externalId());
        return reconcileUseCase.reconcile(item, jobExecutionId);
    }

}
