package com.reconciliation.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Validated
@ConfigurationProperties(prefix = "app.batch")
public record BatchProperties(
                String inputDir,
                String archiveDir,
                @Min(value = 10, message = "Chunk size must be at least 10") @Max(value = 900, message = "Chunk size must be at most 900") int chunkSize,
                @Min(value = 1, message = "Max threads must be at least 1") @Max(value = 10, message = "Max threads must be at most 10") int maxThreads,
                @Min(value = 1, message = "Max retries must be at least 1") @Max(value = 1000, message = "Max retries must be at most 1000") int skipLimit,
                @Min(value = 1, message = "gridSize must be at least 1") @Max(value = 64, message = "gridSize must not exceed 64") int gridSize,
                @Min(value = 1, message = "Retry limit must be at least 1") @Max(value = 1000, message = "Retry limit must be at most 1000") int retryLimit,
                @NotBlank @Pattern(regexp = "single|multithreaded|partitioned", message = "processingMode must be one of: single, multithreaded, partitioned") String processingMode) {
}
