package io.github.pgatzka.videogen.job;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class VideoGenerationTasklet implements Tasklet {

  private final VideoJobService videoJobService;
  private final String jobId;

  public VideoGenerationTasklet(
      VideoJobService videoJobService, @Value("#{jobParameters['jobId']}") String jobId) {
    this.videoJobService = videoJobService;
    this.jobId = jobId;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    log.info("Batch tasklet executing for video job id={}", jobId);
    videoJobService.executeJob(UUID.fromString(jobId));
    log.info("Batch tasklet completed for video job id={}", jobId);
    return RepeatStatus.FINISHED;
  }
}
