package com.springbatch.project.batch.processor;

import java.time.LocalDateTime;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.springbatch.project.domain.dto.TransactionCsvDTO;
import com.springbatch.project.domain.entity.Transaction;

@Component
public class TransactionProcessor implements ItemProcessor<TransactionCsvDTO, Transaction> {

    @Override
    public Transaction process(TransactionCsvDTO item) {
        return Transaction.builder()
                .reference(item.getReference())
                .amount(item.getAmount())
                .currency(item.getCurrency().toUpperCase())
                .transactionDate(item.getTransactionDate())
                .importDate(LocalDateTime.now())
                .build();
    }

}
