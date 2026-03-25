package com.reconciliation.batch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReportFailureTasklet implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var stepContext = chunkContext.getStepContext();
        var stepExecution = stepContext.getStepExecution();
        var jobExecution = stepExecution.getJobExecution();
        var jobParameters = stepContext.getJobParameters();

        String inputFileName = (String) jobParameters.get("inputFileName");
        String runDate = (String) jobParameters.get("runDate");
        long jobExId = jobExecution.getId();
        long totalSkipped = jobExecution.getStepExecutions().stream()
                .mapToLong(se -> se.getSkipCount()).sum();

        log.error("╔══════════════════════════════════════════════════════════╗");
        log.error("║  RECONCILIATION JOB FAILED                               ║");
        log.error("╠══════════════════════════════════════════════════════════╣");
        log.error("║  Job execution ID : {}", jobExId);
        log.error("║  Input file       : {}", inputFileName);
        log.error("║  Run date         : {}", runDate);
        log.error("║  Items skipped    : {}", totalSkipped);
        log.error("╠══════════════════════════════════════════════════════════╣");
        log.error("║  ACTION REQUIRED:                                        ║");
        log.error("║  1. Check logs for CsvParsingException details           ║");
        log.error("║  2. Fix the input file                                   ║");
        log.error("║  3. Re-run the job — Spring Batch will resume from       ║");
        log.error("║     the last committed chunk automatically               ║");
        log.error("╚══════════════════════════════════════════════════════════╝");

        return RepeatStatus.FINISHED;
    }

}
