package com.springbatch.project.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.transaction.PlatformTransactionManager;

import com.springbatch.project.batch.writer.ErrorCsvWriter;
import com.springbatch.project.domain.dto.TransactionCsvDTO;
import com.springbatch.project.listener.TransactionSkipListener;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class TransactionStepConfig {

    @Bean
    public Step transactionStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ItemReader<TransactionCsvDTO> reader, ItemProcessor<TransactionCsvDTO, Object> processor,
            ItemWriter<Object> writer, TransactionSkipListener listener, ErrorCsvWriter errorCsvWriter) {

        return new StepBuilder("transactionStep", jobRepository)
                .<TransactionCsvDTO, Object>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .stream(errorCsvWriter)
                .faultTolerant()
                .skip(NumberFormatException.class)
                .skipLimit(100)
                .retry(RedisConnectionFailureException.class)
                .retryLimit(3)
                .listener(listener)
                .build();
    }
}
