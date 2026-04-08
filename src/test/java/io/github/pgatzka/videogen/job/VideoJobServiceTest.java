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

  @Test
  void createJobPersistsWithQueuedStatus() {
    VideoJobEntity job = service.createJob("BubbleSort", "BarChart", 10, 30, 320, 240, 2, false);

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

    VideoJobEntity job = service.createJob("BubbleSort", "BarChart", 5, 30, 320, 240, 1, false);

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

    VideoJobEntity job = service.createJob("BubbleSort", "BarChart", 5, 30, 320, 240, 1, false);

    service.executeJob(job.getId());

    VideoJobEntity updated = repository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.FAILED);
    assertThat(updated.getErrorMessage()).contains("FFmpeg crashed");
  }

  @Test
  void executeJobWithUnknownAlgorithmFails() throws Exception {
    FfmpegEncoder mockEncoder = mock(FfmpegEncoder.class);
    when(encoderFactory.create()).thenReturn(mockEncoder);

    VideoJobEntity job = service.createJob("UnknownSort", "BarChart", 5, 30, 320, 240, 1, false);

    service.executeJob(job.getId());

    VideoJobEntity updated = repository.findById(job.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.FAILED);
    assertThat(updated.getErrorMessage()).contains("UnknownSort");
  }

  @Test
  void getAllJobsReturnsCreatedJobs() {
    service.createJob("BubbleSort", "BarChart", 10, 30, 320, 240, 2, false);
    service.createJob("QuickSort", "BarChart", 20, 60, 1080, 1920, 3, false);

    List<VideoJobEntity> all = service.getAllJobs();
    assertThat(all).hasSize(2);
  }
}
