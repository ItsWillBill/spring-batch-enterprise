package com.reconciliation.batch.listener;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.reconciliation.shared.exception.CsvParsingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SkipItemListener implements SkipListener<Object, Object> {

    private final JdbcTemplate jdbcTemplate;
    private Long jobExecutionId;
    private String stepName;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.jobExecutionId = stepExecution.getJobExecutionId();
        this.stepName = stepExecution.getStepName();
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Item skipped during READ - {}: {}", t.getClass().getSimpleName(), t.getMessage());
        persistSkip(null, null, t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("Skipped item during write: " + item + ", due to: " + t.getMessage());
        persistSkip(null, item != null ? item.toString() : null, t);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("Skipped item during process: " + item + ", due to: " + t.getMessage());
        persistSkip(null, item != null ? item.toString() : null, t);
    }

    private void persistSkip(Integer lineNumber, String rawLine, Throwable t) {
        if (t instanceof CsvParsingException csvEx) {
            lineNumber = csvEx.getLineNumber();
            rawLine = csvEx.getRawline();
        }

        try {
            jdbcTemplate.update(
                    """
                               INSERT INTO skipped_items (job_execution_id, step_name, line_number, raw_line, error_message)
                               VALUES (?, ?, ?, ?, ?)
                            """,
                    jobExecutionId, stepName, lineNumber, rawLine,
                    t.getMessage() != null ? t.getMessage().substring(0, Math.min(t.getMessage().length(), 1000))
                            : "unknown");
        } catch (Exception e) {
            log.error("Error occurred while persisting skipped item record - jobExecutionId: {}, error: {}",
                    jobExecutionId, e.getMessage());
        }
    }

}
