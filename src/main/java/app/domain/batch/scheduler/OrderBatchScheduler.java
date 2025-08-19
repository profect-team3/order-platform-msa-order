package app.domain.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderBatchScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("orderSyncJob")
    private final Job orderSyncJob;

    @Scheduled(cron = "0 10 * * * *")
    public void runOrderSyncJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("JobID", String.valueOf(System.currentTimeMillis()))
                    .toJobParameters();
            jobLauncher.run(orderSyncJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
