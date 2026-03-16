package com.reconciliation.batch.tasklet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.reconciliation.shared.BatchProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveFileTasklet implements Tasklet {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final BatchProperties batchProperties;

    /**
     * Executes the archive operation
     * 
     * <p>
     * {@link RepeatStatus#FINISHED} signals to Spring Batch that the tasklet has
     * completed its work and does not need to be executed again.
     * {@link RepeatStatus#CONTINUABLE} indicates that the tasklet has more work to
     * do and should be executed again in the next iteration of the step.
     */

    @Override
    @Nullable
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String inputFileName = (String) chunkContext.getStepContext().getJobParameters().get("inputFileName");

        if (inputFileName == null || inputFileName.isBlank()) {
            log.warn("Input file name is missing in JobParameters - skipping archive step.");
            return RepeatStatus.FINISHED;
        }

        Path sourcePath = Paths.get(batchProperties.inputDir(), inputFileName);

        if (!Files.exists(sourcePath)) {
            log.warn("Source file {} does not exist - already archived or deleted.", sourcePath);
            return RepeatStatus.FINISHED;
        }

        String archivedFileName = buildArchivedFileName(inputFileName);
        Path destinationPath = Paths.get(batchProperties.archiveDir(), archivedFileName);

        archiveFile(sourcePath, destinationPath);

        // Tell Spring Batch how many times this tasklet has been executed (for logging
        // and monitoring purposes)
        contribution.incrementWriteCount(1);

        return RepeatStatus.FINISHED;
    }

    /**
     * Appends a timestamp to the filename to ensure uniqueness and prevent
     * overwriting existing files in the archive directory.
     */

    private String buildArchivedFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        int dotIndex = originalFileName.lastIndexOf('.');

        return switch (dotIndex) {
            case -1 -> originalFileName + "_" + timestamp; // No extension
            default -> originalFileName.substring(0, dotIndex) + "_" + timestamp + originalFileName.substring(dotIndex);
        };
    }

    private void archiveFile(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("Archived file {} to {}", source.getFileName(), destination.getFileName());
    }

}
