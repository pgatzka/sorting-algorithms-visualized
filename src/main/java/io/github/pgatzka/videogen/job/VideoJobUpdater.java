package io.github.pgatzka.videogen.job;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoJobUpdater {

  private final VideoJobRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStatusMessage(UUID jobId, String message) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setStatusMessage(message);
              repository.save(job);
              log.info("Job {}: {}", jobId, message);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateProgress(UUID jobId, int progress, String message) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setProgress(progress);
              job.setStatusMessage(message);
              repository.save(job);
              log.info("Job {}: {}% - {}", jobId, progress, message);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateProgress(UUID jobId, int progress) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setProgress(progress);
              repository.save(job);
              log.debug("Job {}: Progress updated to {}%", jobId, progress);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStatus(UUID jobId, VideoJobStatus status) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              log.info("Job {}: Status change {} -> {}", jobId, job.getStatus(), status);
              job.setStatus(status);
              repository.save(job);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markRunning(UUID jobId, java.time.Instant startedAt) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              log.info("Job {}: Status change {} -> RUNNING", jobId, job.getStatus());
              job.setStatus(VideoJobStatus.RUNNING);
              job.setStatusMessage("Initializing...");
              job.setStartedAt(startedAt);
              repository.save(job);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markCompleted(UUID jobId, String outputPath, java.time.Instant completedAt) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              log.info("Job {}: Status change {} -> COMPLETED", jobId, job.getStatus());
              job.setProgress(100);
              job.setStatusMessage("Done");
              job.setOutputPath(outputPath);
              job.setCompletedAt(completedAt);
              job.setStatus(VideoJobStatus.COMPLETED);
              repository.save(job);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID jobId, String errorMessage, java.time.Instant completedAt) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              log.info("Job {}: Status change {} -> FAILED", jobId, job.getStatus());
              job.setStatusMessage("Failed: " + errorMessage);
              job.setErrorMessage(errorMessage);
              job.setCompletedAt(completedAt);
              job.setStatus(VideoJobStatus.FAILED);
              repository.save(job);
            });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateYouTubeStatus(UUID jobId, String videoId, String status) {
    repository
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setYoutubeVideoId(videoId);
              job.setYoutubeStatus(status);
              job.setStatusMessage("YouTube: " + status);
              repository.save(job);
              log.info("Job {}: YouTube status updated to {}", jobId, status);
            });
  }
}
