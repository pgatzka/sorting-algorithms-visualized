package io.github.pgatzka.videogen.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pgatzka.videogen.encoding.FfmpegEncoder;
import io.github.pgatzka.videogen.encoding.FfmpegEncoderFactory;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class VideoGenerationTaskletTest {

  @Autowired private VideoJobService service;
  @Autowired private VideoJobRepository repository;
  @MockitoBean private FfmpegEncoderFactory encoderFactory;

  @Test
  void submitJobLaunchesBatchAndCompletes() throws Exception {
    FfmpegEncoder mockEncoder = mock(FfmpegEncoder.class);
    when(encoderFactory.create()).thenReturn(mockEncoder);

    VideoJobEntity job = service.submitJob("BubbleSort", "BarChart", 5, 30, 320, 240, 1);
    assertThat(job.getId()).isNotNull();

    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () -> {
              VideoJobEntity updated = repository.findById(job.getId()).orElseThrow();
              assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.COMPLETED);
            });

    VideoJobEntity completed = repository.findById(job.getId()).orElseThrow();
    assertThat(completed.getProgress()).isEqualTo(100);
    assertThat(completed.getOutputPath()).isNotNull();
  }

  @Test
  void submitMultipleJobsProcessesBoth() throws Exception {
    FfmpegEncoder mockEncoder = mock(FfmpegEncoder.class);
    when(encoderFactory.create()).thenReturn(mockEncoder);

    VideoJobEntity job1 = service.submitJob("BubbleSort", "BarChart", 5, 30, 320, 240, 1);
    VideoJobEntity job2 = service.submitJob("QuickSort", "BarChart", 5, 30, 320, 240, 1);

    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () -> {
              VideoJobEntity u1 = repository.findById(job1.getId()).orElseThrow();
              VideoJobEntity u2 = repository.findById(job2.getId()).orElseThrow();
              assertThat(u1.getStatus()).isEqualTo(VideoJobStatus.COMPLETED);
              assertThat(u2.getStatus()).isEqualTo(VideoJobStatus.COMPLETED);
            });
  }
}
