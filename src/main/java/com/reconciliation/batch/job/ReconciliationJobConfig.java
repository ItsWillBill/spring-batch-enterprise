package com.reconciliation.batch.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.reconciliation.batch.step.ReconciliationStepConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJobConfig {

    private static final String JOB_NAME = "transactionReconciliationJob";

    private final JobRepository jobRepository;
    private final ReconciliationStepConfig stepConfig;

    @Bean
    public Job transactionReconciliationJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(stepConfig.reconciliationStep())
                .next(stepConfig.archiveFileStep())
                .build();
    }

    /**
     * Build {@link JobParameters} for a given CSV file.
     * <p>
     * The {@code run.id} timestamp parameter ensures that the same file can be
     * re-run after a fix without Spring Batch rejecting it as "already completed".
     */

    public static JobParameters buildJobParameters(String inputFileName) {
        return new JobParametersBuilder()
                .addString("inputFileName", inputFileName)
                .addLong("run.id", System.currentTimeMillis()) // ensure uniqueness per run
                .toJobParameters();
    }
}
