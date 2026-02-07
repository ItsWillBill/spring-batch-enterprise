package com.springbatch.project.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.springbatch.project.domain.dto.TransactionCsvDTO;
import com.springbatch.project.domain.entity.Transaction;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class TransactionStepConfig {

    @Bean
    public Step transactionStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ItemReader<TransactionCsvDTO> reader,
            ItemProcessor<TransactionCsvDTO, Transaction> processor,
            ItemWriter<Transaction> writer) {

        return new StepBuilder("transactionStep", jobRepository)
                .<TransactionCsvDTO, Transaction>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
