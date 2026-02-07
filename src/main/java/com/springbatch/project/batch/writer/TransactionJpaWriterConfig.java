package com.springbatch.project.batch.writer;

import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.springbatch.project.domain.entity.Transaction;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class TransactionJpaWriterConfig {

    @Bean
    public JpaItemWriter<Transaction> transactionWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<Transaction>()
                .entityManagerFactory(emf)
                .usePersist(true)
                .build();
    }
}
