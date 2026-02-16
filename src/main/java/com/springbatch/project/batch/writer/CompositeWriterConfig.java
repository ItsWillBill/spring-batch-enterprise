package com.springbatch.project.batch.writer;

import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.springbatch.project.domain.entity.Transaction;
import com.springbatch.project.domain.error.TransactionError;

@Configuration
public class CompositeWriterConfig {
        @Bean
        public CompositeItemWriter<Object> compositeWriter(JpaItemWriter<Transaction> jpaItemWriter,
                        ErrorCsvWriter errorCsvWriter) {
                CompositeItemWriter<Object> writer = new CompositeItemWriter<>();
                writer.setDelegates(List.of(
                                items -> jpaItemWriter.write(
                                                new Chunk<>(items.getItems().stream()
                                                                .filter(Transaction.class::isInstance)
                                                                .map(Transaction.class::cast)
                                                                .toList())),
                                items -> errorCsvWriter.write(
                                                new Chunk<>(items.getItems().stream()
                                                                .filter(TransactionError.class::isInstance)
                                                                .map(TransactionError.class::cast)
                                                                .toList()))));
                return writer;
        }
}
