package com.reconciliation.batch.step;

import com.reconciliation.batch.listener.SkipItemListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.reconciliation.batch.listener.ChunkMetricsListener;
import com.reconciliation.batch.listener.StepProgressListener;
import com.reconciliation.batch.processor.ReconciliationItemProcessor;
import com.reconciliation.batch.skip.ReconciliationSkipPolicy;
import com.reconciliation.batch.tasklet.ArchiveFileTasklet;
import com.reconciliation.batch.tasklet.ReportFailureTasklet;
import com.reconciliation.batch.writer.ReconciliationResultItemWriter;
import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.Transaction;
import com.reconciliation.shared.BatchProperties;
import com.reconciliation.shared.exception.TransientReconciliationException;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ReconciliationStepConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties batchProperties;
    private final FlatFileItemReader<Transaction> transactionItemReader;
    private final ReconciliationSkipPolicy skipPolicy;
    private final ReconciliationItemProcessor processor;
    private final ReconciliationResultItemWriter Writer;
    private final ArchiveFileTasklet archiveFileTasklet;
    private final ReportFailureTasklet reportFailureTasklet;
    private final StepProgressListener stepProgressListener;
    private final ChunkMetricsListener chunkMetricsListener;
    private final SkipItemListener skipItemListener;

    @Value("${app.batch.retry-limit}")
    private int retryLimit;

    @Bean
    public Step reconciliationStep() {
        return new StepBuilder("reconciliationStep", jobRepository)
                .<Transaction, ReconciliationResult>chunk(batchProperties.chunkSize(), transactionManager)
                .reader(transactionItemReader)
                .processor(processor)
                .writer(Writer)
                .faultTolerant()
                .skipPolicy(skipPolicy)
                .retry(TransientReconciliationException.class)
                .retryLimit(retryLimit)
                .listener(stepProgressListener)
                .listener(chunkMetricsListener)
                .listener(skipItemListener)
                .build();
    }

    @Bean
    public Step archiveFileStep() {
        return new StepBuilder("archiveFileStep", jobRepository)
                .tasklet(archiveFileTasklet, transactionManager)
                .listener(stepProgressListener)
                .build();
    }

    @Bean
    public Step reportFailureStep() {
        return new StepBuilder("reportFailureStep", jobRepository)
                .tasklet(reportFailureTasklet, transactionManager)
                .listener(stepProgressListener)
                .build();
    }
}
