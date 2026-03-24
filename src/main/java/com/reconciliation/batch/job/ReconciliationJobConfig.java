package com.reconciliation.batch.job;

import java.time.LocalDate;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.reconciliation.batch.listener.JobExecutionReportListener;
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
    private final JobExecutionReportListener reportListener;

    @Bean
    public Job transactionReconciliationJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(reportListener)
                .start(stepConfig.reconciliationStep())
                .on("COMPLETED").to(stepConfig.archiveFileStep())
                .on("FAILED").to(stepConfig.reportFailureStep())
                .on("*").to(stepConfig.reconciliationStep())
                .from(stepConfig.archiveFileStep())
                .on("*").end()
                .from(stepConfig.reportFailureStep())
                .on("*").fail()
                .end()
                .build();
    }

    /**
     * Build {@link JobParameters} for a given CSV file.
     * <p>
     * The {@code run.id} timestamp parameter ensures that the same file can be
     * re-run after a fix without Spring Batch rejecting it as "already completed".
     */

    public static JobParameters buildJobParameters(String inputFileName, String runDate) {
        String effectiveRunDate = (runDate != null && !runDate.isBlank()) ? runDate : LocalDate.now().toString();
        return new JobParametersBuilder()
                .addString("inputFileName", inputFileName)
                .addString("runDate", effectiveRunDate)
                .addLong("run.id", System.currentTimeMillis()) // ensure uniqueness per run
                .toJobParameters();
    }
}
