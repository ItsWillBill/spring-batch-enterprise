package com.springbatch.project.listener;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.springbatch.project.batch.writer.ErrorCsvWriter;
import com.springbatch.project.domain.dto.TransactionCsvDTO;
import com.springbatch.project.domain.entity.Transaction;
import com.springbatch.project.domain.error.TransactionError;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class TransactionSkipListener implements SkipListener<TransactionCsvDTO, Transaction> {

    private final ErrorCsvWriter errorCsvWriter;

    @Override
    public void onSkipInProcess(TransactionCsvDTO item, Throwable t) {
        try {
            errorCsvWriter.writeOne(new TransactionError(item, t.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}