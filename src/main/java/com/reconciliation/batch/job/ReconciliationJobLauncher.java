package com.reconciliation.batch.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReconciliationJobLauncher implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job transactionReconciliationJob;

    @Override
    public void run(String... args) throws Exception {
        String inputFileName = null;
        String runDate = null;

        // Parse key=value args — Docker passes each command[] entry as a separate arg
        for (String arg : args) {
            int eq = arg.indexOf('=');
            if (eq < 1) {
                log.warn("Ignoring unrecognised argument '{}' — expected key=value format", arg);
                continue;
            }
            String key = arg.substring(0, eq).trim();
            String value = arg.substring(eq + 1).trim();

            switch (key) {
                case "inputFileName" -> inputFileName = value;
                case "runDate" -> runDate = value;
                default -> log.warn("Ignoring unknown parameter: {}={}", key, value);
            }
        }

        if (inputFileName == null || inputFileName.isBlank()) {
            log.warn("╔══════════════════════════════════════════════════════════╗");
            log.warn("║  No inputFileName provided — job not launched.           ║");
            log.warn("║  Usage:                                                  ║");
            log.warn("║    docker-compose up -e INPUT_FILE=transactions.csv      ║");
            log.warn("║  Or in docker-compose.yml command:                       ║");
            log.warn("║    command: [\"inputFileName=transactions.csv\"]           ║");
            log.warn("╚══════════════════════════════════════════════════════════╝");
            return;
        }

        String effectiveRunDate = (runDate != null && !runDate.isBlank())
                ? runDate
                : LocalDate.now().toString();

        JobParameters params = new JobParametersBuilder()
                .addString("inputFileName", inputFileName)
                .addString("runDate", effectiveRunDate)
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        log.info("Launching job — inputFileName={}, runDate={}", inputFileName, effectiveRunDate);

        JobExecution execution = jobLauncher.run(transactionReconciliationJob, params);

        log.info("Job finished — status={}, exitCode={}",
                execution.getStatus(),
                execution.getExitStatus().getExitCode());
    }
}