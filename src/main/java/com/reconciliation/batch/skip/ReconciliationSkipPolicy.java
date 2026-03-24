package com.reconciliation.batch.skip;

import com.reconciliation.shared.exception.CsvParsingException;
import com.reconciliation.shared.exception.TransientReconciliationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single authority for all skip decisions in the reconciliation step.
 *
 * <h2>Decision matrix</h2>
 * 
 * <pre>
 * Exception received                          │ Action   │ Reason
 * ────────────────────────────────────────────────────────────────────────────
 * FlatFileParseException                      │ SKIP     │ Reader wraps CsvParsingException
 *   └─ cause: CsvParsingException             │          │ — unwrap and log detail
 *   └─ cause: anything else                   │ SKIP     │ — unreadable line, log and move on
 * CsvParsingException (direct)                │ SKIP     │ Bad row from process phase
 * TransientReconciliationException            │ NO SKIP  │ Retryable — goes to retry policy
 * IllegalStateException                       │ NO SKIP  │ Bug in our code — fail fast
 * Any other exception                         │ NO SKIP  │ Unknown — fail fast, investigate
 * ────────────────────────────────────────────────────────────────────────────
 * </pre>
 *
 * <p>
 * If {@code skipCount >= skipLimit}, the step fails regardless of exception
 * type —
 * a catastrophic input file (entirely malformed) must not produce silent zero
 * output.
 *
 * <p>
 * <b>Applying SRP:</b> Makes exactly one decision — skip or not.
 * <p>
 * <b>Applying OCP:</b> New skippable types added here without touching step
 * config.
 */
@Component
@Slf4j
public class ReconciliationSkipPolicy implements SkipPolicy {

    private final int skipLimit;

    public ReconciliationSkipPolicy(@Value("${app.batch.skip-limit:10}") int skipLimit) {
        this.skipLimit = skipLimit;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {

        if (skipCount >= skipLimit) {
            log.error(
                    "Skip limit exhausted — skipped={}, limit={} — failing step. " +
                            "Last exception: {}({})",
                    skipCount, skipLimit,
                    t.getClass().getSimpleName(), t.getMessage());
            throw new SkipLimitExceededException(Math.toIntExact(skipCount), t);
        }

        // ── Read-phase: FlatFileItemReader wraps LineMapper exceptions ────────
        // Java 21: pattern matching instanceof — no redundant cast needed
        if (t instanceof FlatFileParseException parseEx) {
            Throwable cause = parseEx.getCause();
            if (cause instanceof CsvParsingException csvEx) {
                log.warn(
                        "Skipping malformed CSV row [{}/{}] — line={}, reason={}",
                        skipCount + 1, skipLimit, csvEx.getLineNumber(), csvEx.getMessage());
            } else {
                log.warn(
                        "Skipping unparseable line [{}/{}] — line={}, reason={}",
                        skipCount + 1, skipLimit, parseEx.getLineNumber(), parseEx.getMessage());
            }
            return true;
        }

        // ── Process-phase: CsvParsingException thrown directly ───────────────
        if (t instanceof CsvParsingException csvEx) {
            log.warn(
                    "Skipping malformed item [{}/{}] — line={}, reason={}",
                    skipCount + 1, skipLimit, csvEx.getLineNumber(), csvEx.getMessage());
            return true;
        }

        // ── Retryable — defer to retry policy ────────────────────────────────
        if (t instanceof TransientReconciliationException) {
            log.debug("TransientReconciliationException deferred to retry policy: {}", t.getMessage());
            return false;
        }

        // ── Everything else — fail fast ───────────────────────────────────────
        log.error(
                "Non-skippable exception — failing step immediately. type={}, message={}",
                t.getClass().getSimpleName(), t.getMessage());
        return false;
    }
}