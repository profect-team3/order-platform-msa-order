package app.domain.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
public class OrderBatchController {

    private final JobLauncher jobLauncher;
    @Qualifier("orderSyncJob")
    private final Job orderMigrationJob;

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> triggerOrderMigration() {
        Map<String, Object> response = new HashMap<>();
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobId", UUID.randomUUID().toString())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(orderMigrationJob, jobParameters);

            response.put("jobId", jobExecution.getJobId());
            response.put("status", jobExecution.getStatus());
            response.put("message", "Order migration batch job started successfully.");
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException e) {
            response.put("message", "Job is already running: " + e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            response.put("message", "Error while starting job: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
