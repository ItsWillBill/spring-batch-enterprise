package com.springbatch.project.batch.processor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.springbatch.project.domain.dto.TransactionCsvDTO;
import com.springbatch.project.domain.entity.Transaction;
import com.springbatch.project.domain.error.TransactionError;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class TransactionProcessor implements ItemProcessor<TransactionCsvDTO, Object> {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Object process(TransactionCsvDTO item) {
        String key = "tx:" + item.getReference();
        Boolean alreadyProcessed = redisTemplate.opsForValue().setIfAbsent(key, "1");

        if (Boolean.FALSE.equals(alreadyProcessed)) {
            return new TransactionError(item, "DUPLICATE_TRANSACTION");
        }

        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new TransactionError(item, "INVALID_AMOUNT");
        }

        return Transaction.builder()
                .reference(item.getReference())
                .amount(item.getAmount())
                .currency(item.getCurrency().toUpperCase())
                .transactionDate(item.getTransactionDate())
                .importDate(LocalDateTime.now())
                .build();
    }

}
