package com.reconciliation.batch.partitioner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.reconciliation.shared.BatchProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Partition a CSV into N-independent line-range slices for parallel processing.
 */

@Component
@StepScope
@Slf4j
public class CsvFilePartitioner implements Partitioner {

    private final BatchProperties batchProperties;
    private final String inputFileName;

    public CsvFilePartitioner(
            BatchProperties batchProperties,
            @Value("#{jobParameters['inputFileName']}") String inputFileName) {
        this.batchProperties = batchProperties;
        this.inputFileName = inputFileName;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Path filePath = Paths.get(batchProperties.inputDir(), inputFileName);
        long totalLines = countDataLines(filePath);

        if (totalLines == 0) {
            log.warn("CSV file has no data rows - creating single empty partition");
            return singleEmptyPartition();
        }

        // Respect gridSize but don't create more partitions than rows
        int effectiveGridSize = (int) Math.min(gridSize, totalLines);
        long linesPerPartition = (long) Math.ceil((double) totalLines / effectiveGridSize);

        log.info("Partitioning file '{}' - totalLines={}, gridSize={}, linesPerPartition={}",
                filePath, totalLines, gridSize, linesPerPartition);

        Map<String, ExecutionContext> partitions = new HashMap<>();

        for (int i = 0; i < effectiveGridSize; i++) {
            long startLine = i * linesPerPartition + 2; // +2 to skip header and convert to 1-based
            long endLine = Math.min(startLine + linesPerPartition - 1, totalLines + 1); // -1 to convert to 0-based

            ExecutionContext context = new ExecutionContext();
            context.putLong("startLine", startLine);
            context.putLong("endLine", endLine);
            context.putInt("partitionIndex", i);

            String partitionName = "partition_" + i;

            partitions.put(partitionName, context);

            log.debug("Partition[{}]: lines {}-{}", i, startLine, endLine);
        }
        return partitions;
    }

    private long countDataLines(Path filePath) {
        try {
            try (var lines = Files.lines(filePath)) {
                long total = lines.count();
                return Math.max(0, total - 1); // Exclude header line
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot count lines in file " + filePath + " - " + e.getMessage(), e);
        }
    }

    private Map<String, ExecutionContext> singleEmptyPartition() {
        ExecutionContext context = new ExecutionContext();
        context.putLong("startLine", 2);
        context.putLong("endLine", 1); // end < start -> no lines to process
        context.putInt("partitionIndex", 0);
        return Map.of("partition0", context);
    }

}
