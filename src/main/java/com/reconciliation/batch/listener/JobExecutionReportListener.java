package com.reconciliation.batch.listener;

import java.time.Duration;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Job-level listener - fires once before the job starts and once after it ends.
 */

@Component
@Slf4j
public class JobExecutionReportListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║  JOB STARTING                                            ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║  Job name     : {}", jobExecution.getJobInstance().getJobName());
        log.info("║  Execution ID : {}", jobExecution.getId());
        log.info("║  Parameters   : {}", jobExecution.getJobParameters());
        log.info("╚══════════════════════════════════════════════════════════╝");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long totalRead = jobExecution.getStepExecutions().stream().mapToLong(s -> s.getReadCount()).sum();
        long totalWritten = jobExecution.getStepExecutions().stream().mapToLong(s -> s.getWriteCount()).sum();
        long totalSkipped = jobExecution.getStepExecutions().stream().mapToLong(s -> s.getSkipCount()).sum();
        long totalFiltered = jobExecution.getStepExecutions().stream().mapToLong(s -> s.getFilterCount()).sum();

        Duration duration = Duration.ZERO;
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            duration = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
        }

        String statusSymbol = switch (jobExecution.getStatus()) {
            case COMPLETED -> "✅";
            case FAILED -> "❌";
            case STOPPED -> "⏹";
            default -> "⚠️";
        };

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  JOB COMPLETED {}                                        ║", statusSymbol);
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║  Job name     : {}", jobExecution.getJobInstance().getJobName());
        log.info("║  Execution ID : {}", jobExecution.getId());
        log.info("║  Status       : {}", jobExecution.getStatus());
        log.info("║  Exit code    : {}", jobExecution.getExitStatus().getExitCode());
        log.info("║  Duration     : {}s", duration.toSeconds());
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║  READ         : {}", totalRead);
        log.info("║  WRITTEN      : {}", totalWritten);
        log.info("║  SKIPPED      : {}", totalSkipped);
        log.info("║  FILTERED     : {}", totalFiltered);
        log.info("╚══════════════════════════════════════════════════════════╝");

        // Log failures separately so it's easier to spot in
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            log.error("Job failure exceptions:");
            jobExecution.getAllFailureExceptions()
                    .forEach(ex -> log.error(" - {}: {}", ex.getClass().getSimpleName(), ex.getMessage()));
        }
    }

}
