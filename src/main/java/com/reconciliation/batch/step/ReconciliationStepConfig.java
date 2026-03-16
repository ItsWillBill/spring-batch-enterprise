package com.reconciliation.batch.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.reconciliation.batch.processor.ReconciliationItemProcessor;
import com.reconciliation.batch.reader.TransactionCsvItemReader;
import com.reconciliation.batch.tasklet.ArchiveFileTasklet;
import com.reconciliation.batch.writer.ReconciliationResultItemWriter;
import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.Transaction;
import com.reconciliation.shared.BatchProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ReconciliationStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties batchProperties;

    private final TransactionCsvItemReader csvItemReader;
    private final ReconciliationItemProcessor processor;
    private final ReconciliationResultItemWriter Writer;
    private final ArchiveFileTasklet archiveFileTasklet;

    @Bean
    public Step reconciliationStep() {
        return new StepBuilder("reconciliationStep", jobRepository)
                .<Transaction, ReconciliationResult>chunk(batchProperties.chunkSize(), transactionManager)
                .reader(csvItemReader.reader(null))
                .processor(processor)
                .writer(Writer)
                .build();
    }

    @Bean
    public Step archiveFileStep() {
        return new StepBuilder("archiveFileStep", jobRepository)
                .tasklet(archiveFileTasklet, transactionManager)
                .build();
    }
}
