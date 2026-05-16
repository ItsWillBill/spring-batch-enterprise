package com.reconciliation.batch.reader;

import java.io.File;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.reconciliation.domain.model.Transaction;
import com.reconciliation.infrastructure.csv.TransactionCsvMapper;
import com.reconciliation.shared.BatchProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionCsvItemReader {

    private final BatchProperties batchProperties;
    private final TransactionCsvMapper transactionCsvMapper;

    @Bean
    @StepScope
    public FlatFileItemReader<Transaction> reader(@Value("#{jobParameters['inputFileName']}") String intputFileName) {

        File inputFile = new File(batchProperties.inputDir(), intputFileName);

        log.info("Configuring CSV reader for file: {} ", inputFile.getAbsolutePath());

        return new FlatFileItemReaderBuilder<Transaction>()
                .name("transactionCsvItemReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(1)
                .saveState(true)
                .lineMapper((line, lineNumber) -> {
                    String[] fields = line.split(",", -1);
                    return transactionCsvMapper.map(fields, lineNumber);
                }).build();
    }

    /**
     * Partition-aware reader - used bu worker steps in the partioned step.
     * Reads only the lines assigned to this partition (startLine to endLine) as
     * defined in the execution context.
     */

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Transaction> partitionTransactionItemReader(
            @Value("#{jobParameters['inputFileName']}") String intputFileName,
            @Value("#{stepExecutionContext['startLine']}") long startLine,
            @Value("#{stepExecutionContext['endLine']}") long endLine) {

        File inputFile = new File(batchProperties.inputDir(), intputFileName);

        log.info("Configuring paritioned CSV reader : file={}, lines={}-{}", inputFile.getAbsolutePath(), startLine,
                endLine);

        // startline=2-> skip 1 line (header only)
        // startline=1001-> skip 1000 lines (header + 1 previous partitions' data)
        int linesToSkip = (int) (startLine - 1);
        int maxItemCount = (int) (endLine - startLine + 1);

        FlatFileItemReader<Transaction> delegate = new FlatFileItemReaderBuilder<Transaction>()
                .name("partitionedTransactionCsvItemReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(linesToSkip)
                .saveState(false) // each partition manages its own state
                .maxItemCount(maxItemCount)
                .lineMapper((line, lineNumber) -> transactionCsvMapper.map(line.split(",", -1), lineNumber))
                .build();

        return new SynchronizedItemStreamReaderBuilder<Transaction>()
                .delegate(delegate)
                .build();
    }
}
