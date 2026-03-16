package com.reconciliation.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.batch")
public record BatchProperties(
                String inputDir,
                String archiveDir,
                @Min(value = 10, message = "Chunk size must be at least 10") @Max(value = 900, message = "Chunk size must be at most 900") int chunkSize,

                @Min(value = 1, message = "Max threads must be at least 1") @Max(value = 10, message = "Max threads must be at most 10") int maxThreads) {
}
