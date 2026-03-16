package com.reconciliation.batch.reader;

import java.io.File;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
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
}
