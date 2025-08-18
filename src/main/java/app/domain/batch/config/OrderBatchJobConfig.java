package app.domain.batch.config;

import app.domain.batch.dto.OrderBatchDto;
import app.domain.batch.job.OrderBatchProcessor;
import app.domain.batch.job.OrderBatchReader;
import app.domain.batch.job.OrderBatchWriter;
import app.domain.batch.mongo.entity.MongoOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class OrderBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderBatchReader orderBatchReader;
    private final OrderBatchProcessor orderBatchProcessor;
    private final OrderBatchWriter orderBatchWriter;

    @Bean
    public Job orderSyncJob() {
        return new JobBuilder("orderSyncJob", jobRepository)
                .start(orderSyncStep())
                .build();
    }

    @Bean
    public Step orderSyncStep() {
        return new StepBuilder("orderSyncStep", jobRepository)
                .<OrderBatchDto, MongoOrder>chunk(100, transactionManager)
                .reader(orderBatchReader)
                .processor(orderBatchProcessor)
                .writer(orderBatchWriter)
                .build();
    }
}
