package com.springbatch.project.batch.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.springbatch.project.domain.error.TransactionError;

@Component
@StepScope
public class ErrorCsvWriter implements ItemWriter<TransactionError>, ItemStream {

    private static final String ERROR_COUNT_KEY = "error.count";
    private static final String HEADER_WRITTEN_KEY = "error.header.written";

    private BufferedWriter writer;
    private long errorCount;

    @Value("#{jobParameters['errorFile']}")
    private Resource errorFile;

    @Override
    public synchronized void write(Chunk<? extends TransactionError> items) throws Exception {
        for (TransactionError error : items) {
            writeSingleLine(error);
        }
        writer.flush();
    }

    public synchronized void writeOne(TransactionError error) throws IOException {
        writeSingleLine(error);
        writer.flush();
    }

    private void writeSingleLine(TransactionError error) throws IOException {
        writer.write(error.source().getReference() + "," + error.reason());
        writer.newLine();
        errorCount++;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        boolean headerWritten = executionContext.containsKey(HEADER_WRITTEN_KEY)
                && (Boolean) executionContext.get(HEADER_WRITTEN_KEY);
        try {
            String projectRoot = System.getProperty("user.dir");
            String relativePath = errorFile.getFilename();
            Path errorFilePath = Paths.get(projectRoot, relativePath);

            if (errorFilePath.getParent() != null) {
                Files.createDirectories(errorFilePath.getParent());
            }
            this.writer = Files.newBufferedWriter(errorFilePath, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            if (!headerWritten) {
                writer.write("transaction_id,reason");
                writer.newLine();
                writer.flush();
                executionContext.put(HEADER_WRITTEN_KEY, true);
            }
        } catch (IOException e) {
            throw new ItemStreamException("Error opening error file", e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext)
            throws ItemStreamException {
        executionContext.putLong(ERROR_COUNT_KEY, errorCount);
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (this.writer != null) {
                this.writer.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException("Error closing error file", e);
        }
    }
}