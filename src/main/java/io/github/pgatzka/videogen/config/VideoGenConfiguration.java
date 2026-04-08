package io.github.pgatzka.videogen.config;

import io.github.pgatzka.videogen.job.VideoGenerationTasklet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableConfigurationProperties(VideoGenProperties.class)
public class VideoGenConfiguration {

  public VideoGenConfiguration(VideoGenProperties properties) {
    log.info(
        "VideoGen configuration loaded: outputDir={}, ffmpegPath={}, "
            + "defaultResolution={}x{}, defaultFps={}, defaultElementCount={}, defaultFramesPerStep={}",
        properties.getOutputDir(),
        properties.getFfmpegPath(),
        properties.getDefaultWidth(),
        properties.getDefaultHeight(),
        properties.getDefaultFps(),
        properties.getDefaultElementCount(),
        properties.getDefaultFramesPerStep());
  }

  @Bean
  public Job videoGenerationJob(JobRepository jobRepository, Step generateVideoStep) {
    return new JobBuilder("videoGenerationJob", jobRepository).start(generateVideoStep).build();
  }

  @Bean
  public Step generateVideoStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      VideoGenerationTasklet tasklet) {
    return new StepBuilder("generateVideo", jobRepository)
        .tasklet(tasklet, transactionManager)
        .build();
  }

  @Bean
  public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
    TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
    launcher.setJobRepository(jobRepository);
    launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("video-gen-"));
    launcher.afterPropertiesSet();
    log.info("Async job launcher configured for video generation");
    return launcher;
  }
}
