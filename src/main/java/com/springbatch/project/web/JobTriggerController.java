package com.springbatch.project.web;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
public class JobTriggerController {

    private final JobLauncher jobLauncher;
    private final Job transactionJob;

    @GetMapping("batch/run")
    public ResponseEntity<String> runJob(@RequestParam String inputFile, @RequestParam String errorFile)
            throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("inputFile", inputFile)
                .addString("errorFile", errorFile)
                .toJobParameters();

        jobLauncher.run(transactionJob, jobParameters);
        return ResponseEntity.ok("Transaction job has been triggered");
    }
}
