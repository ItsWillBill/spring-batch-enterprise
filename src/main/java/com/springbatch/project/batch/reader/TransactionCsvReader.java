package com.springbatch.project.batch.reader;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.springbatch.project.domain.dto.TransactionCsvDTO;

@Configuration
public class TransactionCsvReader {

    @Bean
    public FlatFileItemReader<TransactionCsvDTO> transactionReader() {
        return new FlatFileItemReaderBuilder<TransactionCsvDTO>()
                .name("transactionCsvReader")
                .resource(new ClassPathResource("data/transaction.csv"))
                .linesToSkip(1)
                .delimited()
                .names(
                        "transactionId", "accountNumber", "amount", "currency", "transactionType", "status",
                        "transactionDate", "description")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<TransactionCsvDTO>() {
                    {
                        setTargetType(TransactionCsvDTO.class);
                        setDistanceLimit(0);
                    }
                })
                .build();
    }
}
