package com.reconciliation.batch.job;

import com.reconciliation.shared.BatchProperties;
import java.time.LocalDate;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.reconciliation.batch.listener.JobExecutionReportListener;
import com.reconciliation.batch.step.PartitionedStepConfig;
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
    private final PartitionedStepConfig partitionedStepConfig;
    private final JobExecutionReportListener reportListener;
    private final BatchProperties batchProperties;

    @Bean
    public Job transactionReconciliationJob() {
        Step processingStep = resolveProcessingStep();
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(reportListener)
                .start(processingStep)
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
     * Selects the processing step based on {@code app.batch.processing-mode}
     * property.
     */

    private Step resolveProcessingStep() {
        log.info("Selected processing mode: {}", batchProperties.processingMode());
        return switch (batchProperties.processingMode().toLowerCase()) {
            case "multithreaded" -> {
                log.info("Processing mode : MULTI-THREADED (threads={})", batchProperties.maxThreads());
                yield stepConfig.multiThreadReconciliationStep();
            }
            case "partitioned" -> {
                log.info("Processing mode : PARTITIONED (gridSize = {}, threads={})", batchProperties.gridSize(),
                        batchProperties.maxThreads());
                yield partitionedStepConfig.masterPartitionStep();
            }
            case "single", "" -> {
                log.info("Processing mode : SINGLE-THREADED");
                yield stepConfig.reconciliationStep();
            }
            default -> {
                log.warn("Unknown processing mode '{}', falling back to single-threaded",
                        batchProperties.processingMode());
                yield stepConfig.reconciliationStep();
            }
        };
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
