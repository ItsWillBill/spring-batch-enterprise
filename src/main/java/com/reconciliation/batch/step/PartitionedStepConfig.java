package com.reconciliation.batch.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.reconciliation.batch.listener.ChunkMetricsListener;
import com.reconciliation.batch.listener.SkipItemListener;
import com.reconciliation.batch.listener.StepProgressListener;
import com.reconciliation.batch.partitioner.CsvFilePartitioner;
import com.reconciliation.batch.processor.ReconciliationItemProcessor;
import com.reconciliation.batch.skip.ReconciliationSkipPolicy;
import com.reconciliation.batch.writer.ReconciliationResultItemWriter;
import com.reconciliation.domain.model.ReconciliationResult;
import com.reconciliation.domain.model.Transaction;
import com.reconciliation.shared.BatchProperties;
import com.reconciliation.shared.exception.TransientReconciliationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PartitionedStepConfig {
    private final Step archiveFileStep;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties batchProperties;
    private final SynchronizedItemStreamReader<Transaction> partitionedReader;
    private final ReconciliationItemProcessor processor;
    private final ReconciliationResultItemWriter writer;
    private final ReconciliationSkipPolicy skipPolicy;
    private final StepProgressListener stepProgressListener;
    private final ChunkMetricsListener chunkMetricsListener;
    private final SkipItemListener skipItemListener;
    private final CsvFilePartitioner partitioner;

    @Value("${app.batch.retry-limit}")
    private int retryLimit;

    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .<Transaction, ReconciliationResult>chunk(batchProperties.chunkSize(), transactionManager)
                .reader(partitionedReader)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(skipPolicy)
                .retry(TransientReconciliationException.class)
                .retryLimit(retryLimit)
                .listener(stepProgressListener)
                .listener(chunkMetricsListener)
                .listener(skipItemListener)
                .build();
    }

    /**
     * Master partitioned step - divides the CSV file and dispatches worker steps.
     */

    @Bean
    public Step masterPartitionStep() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setTaskExecutor(partitionTaskExecutor());
        partitionHandler.setStep(workerStep());
        partitionHandler.setGridSize(batchProperties.gridSize());

        try {
            partitionHandler.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize partition handler", e);
        }

        log.info("Partioned step configured - gridSize={}, threads={}", batchProperties.gridSize(),
                batchProperties.maxThreads());

        return new StepBuilder("masterPartitionStep", jobRepository)
                .partitioner("workerStep", partitioner)
                .partitionHandler(partitionHandler)
                .listener(stepProgressListener)
                .build();
    }

    /**
     * Thread pool for parallel partition execution.
     * 
     * <p>
     * Core size = max sie = gridSize - avoirds thread creation overhead.
     * Queue capacity = 0 - partitions execute immediately or wait (no undounded
     * queue).
     * {@code awaitTerminationSeconds} - allows in-flight chunks to complete on
     * shutdown.
     */

    @Bean
    public TaskExecutor partitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchProperties.maxThreads());
        executor.setMaxPoolSize(batchProperties.maxThreads());
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("partition-worker-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();

        log.info("Partition TaskExecutor intialised - threads={}", batchProperties.maxThreads());

        return executor;
    }
}
