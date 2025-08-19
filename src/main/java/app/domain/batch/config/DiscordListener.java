package app.domain.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DiscordListener implements JobExecutionListener {

    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String message = String.format(
            "**배치 잡 시작 알림**\n" +
            "**잡 이름**: `%s`\n" +
            "**시작 시간**: `%s`",
            jobName,
            jobExecution.getStartTime()
        );
        sendMessageToDiscord(message);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().name();
        String exitMessage = jobExecution.getExitStatus().getExitDescription();

        long readCount = 0;
        long writeCount = 0;
        long filterCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long writeSkipCount = 0;

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            readCount += stepExecution.getReadCount();
            writeCount += stepExecution.getWriteCount();
            filterCount += stepExecution.getFilterCount();
            readSkipCount += stepExecution.getReadSkipCount();
            processSkipCount += stepExecution.getProcessSkipCount();
            writeSkipCount += stepExecution.getWriteSkipCount();
        }

        long totalSkipCount = readSkipCount + processSkipCount + writeSkipCount;
        int failureCount = jobExecution.getAllFailureExceptions().size();

        String durationString = "N/A";
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            Duration duration = Duration.between(
                jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant(),
                jobExecution.getEndTime().atZone(ZoneId.systemDefault()).toInstant()
            );
            durationString = String.format("%d분 %d초", duration.toMinutes(), duration.toSeconds() % 60);
        }

        String message = String.format(
            "**배치 잡 완료 알림**\n" +
            "**잡 이름**: `%s`\n" +
            "**상태**: `%s`\n" +
            "**시작 시간**: `%s`\n" +
            "**종료 시간**: `%s`\n" +
            "**소요 시간**: `%s`\n\n" +
            "**처리 결과:**\n" +
            "- **읽은 아이템**: `%d`\n" +
            "- **처리된 아이템**: `%d`\n" +
            "- **필터링된 아이템**: `%d`\n" +
            "- **스킵된 아이템 (읽기/처리/쓰기)**: `%d` (`%d`/`%d`/`%d`)\n" +
            "- **총 실패 수 (예외)**: `%d`\n\n" +
            "**종료 메시지**: `%s`",
            jobName, status,
            jobExecution.getStartTime(), jobExecution.getEndTime(),
            durationString,
            readCount, writeCount, filterCount,
            totalSkipCount, readSkipCount, processSkipCount, writeSkipCount,
            failureCount,
            exitMessage
        );

        sendMessageToDiscord(message);
    }

    private void sendMessageToDiscord(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("content", message);

        try {
            restTemplate.postForEntity(discordWebhookUrl, payload, String.class);
        } catch (Exception e) {
            log.error("Discord 알림 전송 실패: {}", e.getMessage(), e);
        }
    }
}
