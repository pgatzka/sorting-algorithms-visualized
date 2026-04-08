package io.github.pgatzka.videogen.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.github.pgatzka.videogen.encoding.FfmpegEncoder;
import io.github.pgatzka.videogen.encoding.FfmpegEncoderFactory;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class VideoJobServiceTest {

  @Autowired private VideoJobService service;
  @Autowired private VideoJobRepository repository;
  @MockitoBean private FfmpegEncoderFactory encoderFactory;

  @AfterEach
  void cleanup() {
    repository.deleteAll();
  }

  private static VideoJobRequest defaultRequest() {
    return VideoJobRequest.builder()
        .algorithm("BubbleSort")
        .visualization("BarChart")
        .elementCount(5)
        .fps(30)
        .width(320)
        .height(240)
        .framesPerStep(1)
        .colorScheme("DEFAULT")
        .build();
  }

  @Test
  void createJobPersistsWithQueuedStatus() {
    VideoJobRequest request =
        VideoJobRequest.builder()
            .algorithm("BubbleSort")
            .visualization("BarChart")
            .elementCount(10)
            .fps(30)
            .width(320)
            .height(240)
            .framesPerStep(2)
            .colorScheme("DEFAULT")
            .build();

    VideoJobEntity job = service.createJob(request);

    assertThat(job.getId()).isNotNull();
    assertThat(job.getStatus()).isEqualTo(VideoJobStatus.QUEUED);
    assertThat(job.getAlgorithm()).isEqualTo("BubbleSort");
    assertThat(job.getVisualization()).isEqualTo("BarChart");
    assertThat(job.getElementCount()).isEqualTo(10);
    assertThat(job.getProgress()).isZero();
  }

  @Test
  void executeJobTransitionsToCompleted() throws Exception {
    FfmpegEncoder mockEncoder = mock(FfmpegEncoder.class);
    when(encoderFactory.create()).thenReturn(mockEncoder);

    VideoJobEntity job = service.createJob(defaultRequest());

    service.executeJob(job.getId());

    VideoJobEntity updated = repository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.COMPLETED);
    assertThat(updated.getProgress()).isEqualTo(100);
    assertThat(updated.getOutputPath()).isNotNull();
    assertThat(updated.getStartedAt()).isNotNull();
    assertThat(updated.getCompletedAt()).isNotNull();

    verify(mockEncoder).start(any(), anyInt(), anyInt(), anyInt(), anyString());
    verify(mockEncoder, atLeastOnce()).writeFrame(any());
    verify(mockEncoder).close();
  }

  @Test
  void executeJobWithEncoderErrorTransitionsToFailed() throws Exception {
    FfmpegEncoder mockEncoder = mock(FfmpegEncoder.class);
    when(encoderFactory.create()).thenReturn(mockEncoder);
    doThrow(new RuntimeException("FFmpeg crashed"))
        .when(mockEncoder)
        .start(any(), anyInt(), anyInt(), anyInt(), anyString());

    VideoJobEntity job = service.createJob(defaultRequest());

    service.executeJob(job.getId());

    VideoJobEntity updated = repository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.FAILED);
    assertThat(updated.getErrorMessage()).contains("FFmpeg crashed");
  }

  @Test
  void executeJobWithUnknownAlgorithmFails() throws Exception {
    FfmpegEncoder mockEncoder = mock(FfmpegEncoder.class);
    when(encoderFactory.create()).thenReturn(mockEncoder);

    VideoJobRequest request =
        VideoJobRequest.builder()
            .algorithm("UnknownSort")
            .visualization("BarChart")
            .elementCount(5)
            .fps(30)
            .width(320)
            .height(240)
            .framesPerStep(1)
            .colorScheme("DEFAULT")
            .build();

    VideoJobEntity job = service.createJob(request);

    service.executeJob(job.getId());

    VideoJobEntity updated = repository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.FAILED);
    assertThat(updated.getErrorMessage()).contains("UnknownSort");
  }

  @Test
  void getAllJobsReturnsCreatedJobs() {
    service.createJob(defaultRequest());
    service.createJob(
        VideoJobRequest.builder()
            .algorithm("QuickSort")
            .visualization("BarChart")
            .elementCount(20)
            .fps(60)
            .width(1080)
            .height(1920)
            .framesPerStep(3)
            .colorScheme("DEFAULT")
            .build());

    List<VideoJobEntity> all = service.getAllJobs();
    assertThat(all).hasSize(2);
  }
}
