package com.springbatch.project.batch.reader;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.springbatch.project.domain.dto.TransactionCsvDTO;

@Configuration
public class TransactionCsvReader {

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionCsvDTO> transactionReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<TransactionCsvDTO>()
                .name("transactionCsvReader")
                .resource(new ClassPathResource("data/transactions.csv"))
                .linesToSkip(1)
                .saveState(true)
                .delimited()
                .includedFields(0, 2, 3, 6)
                .names(
                        "reference", "amount", "currency", "transactionDate")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<TransactionCsvDTO>() {
                    {
                        setTargetType(TransactionCsvDTO.class);
                        setDistanceLimit(1);
                        setConversionService(new DefaultFormattingConversionService());
                    }
                })
                .build();
    }
}
