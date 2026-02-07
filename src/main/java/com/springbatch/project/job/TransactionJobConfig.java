package com.springbatch.project.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionJobConfig {

    @Bean
    public Job transactionJob(JobRepository jobRepository, Step transactionStep) {
        return new JobBuilder("transactionJob", jobRepository)
                .start(transactionStep)
                .build();
    }
}
