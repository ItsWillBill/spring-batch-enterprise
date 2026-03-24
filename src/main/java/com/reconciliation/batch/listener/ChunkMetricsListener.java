package com.reconciliation.batch.listener;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChunkMetricsListener implements ChunkListener {

    // because multiple threads may process chunks concurrently in phase 3
    private final ThreadLocal<Long> chunkStartTime = new ThreadLocal<>();

    @Override
    public void beforeChunk(ChunkContext context) {
        chunkStartTime.set(System.currentTimeMillis());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        Long startTime = chunkStartTime.get();
        if (startTime == null)
            return;

        long elapsed = System.currentTimeMillis() - startTime;
        long chunkNumber = context.getStepContext().getStepExecution().getCommitCount() + 1;

        log.debug("Chunk #{} completed in {} ms", chunkNumber, elapsed);

        // Warn if a single chunk takes longer than 5 seconds — signals a tuning issue
        if (elapsed > 5_000) {
            log.warn("Slow chunk detected: chunk #{} took {}ms — consider reducing chunk size or " +
                    "investigating slow ledger lookups", chunkNumber, elapsed);
        }

        chunkStartTime.remove(); // Prevent ThreadLocal leak
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        Long startTime = chunkStartTime.get();
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;
        log.error("Chunk error after {}ms in step [{}]", elapsed, context.getStepContext().getStepName());
        chunkStartTime.remove();
    }

}
