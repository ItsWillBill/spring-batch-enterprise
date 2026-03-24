package com.reconciliation.batch.listener;

import java.time.Duration;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StepProgressListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting step: [{}] (jobExecutionId: {})", stepExecution.getStepName(),
                stepExecution.getJobExecution().getId());
    }

    /**
     * Fires after the step completes - whether successful, failed or stopped.
     * Returns the original{@link ExitStatus} unchanged - we are observing only,
     * not modifying the step outcome.
     */

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Duration duration = Duration.ZERO;
        if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
            duration = Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime());
        }

        log.info("── Step completed: [{}] | status={} | duration={}s | " +
                "read={} write={} skip={} filter={} rollback={}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                duration.toSeconds(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getFilterCount(),
                stepExecution.getRollbackCount());

        if (stepExecution.getSkipCount() > 0) {
            log.warn("Step [{}] had {} skipped items. check logs above for CsvParsingException details",
                    stepExecution.getStepName(), stepExecution.getSkipCount());
        }

        return null; // returning null means "don't change the exit status determined by the step's
                     // processing"
    }
}
