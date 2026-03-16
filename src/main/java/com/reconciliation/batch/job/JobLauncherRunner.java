package com.reconciliation.batch.job;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobLauncherRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final ReconciliationJobConfig jobConfig;

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0 || args[0].isBlank()) {
            log.warn("No input file specified. Usage: java -jar reconciliation-engine.jar <filename.csv>");
            log.warn("Application started in standby mode. No job launched.");
            return;
        }

        String inputFileName = args[0];
        log.info("Launching reconciliation Job for file: {}", inputFileName);

        JobParameters params = ReconciliationJobConfig.buildJobParameters(inputFileName);
        var job = jobConfig.transactionReconciliationJob();

        var execution = jobLauncher.run(job, params);

        log.info("Job completed - Status: {}, Exit Status: {}", execution.getStatus(), execution.getExitStatus());
    }

}
