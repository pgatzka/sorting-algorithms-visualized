package io.github.pgatzka.videogen.job;

import io.github.pgatzka.ApplicationProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class VideoCleanupScheduler {

  private final VideoJobRepository repository;
  private final ApplicationProperties properties;

  public VideoCleanupScheduler(VideoJobRepository repository, ApplicationProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
  @Transactional
  public void cleanupOldVideos() {
    int retentionDays = properties.getVideoRetentionDays();
    if (retentionDays <= 0) {
      return;
    }

    Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

    List<VideoJobEntity> oldJobs =
        repository.findAll().stream()
            .filter(job -> job.getCreatedAt() != null && job.getCreatedAt().isBefore(cutoff))
            .filter(job -> job.getOutputPath() != null)
            .toList();

    if (oldJobs.isEmpty()) {
      return;
    }

    log.info("Cleaning up {} videos older than {} days", oldJobs.size(), retentionDays);

    int deleted = 0;
    for (VideoJobEntity job : oldJobs) {
      try {
        Path videoFile = Path.of(job.getOutputPath());
        if (Files.exists(videoFile)) {
          Files.delete(videoFile);
          deleted++;
          log.debug("Deleted video file: {}", videoFile);
        }
        job.setOutputPath(null);
        job.setStatusMessage("Video file cleaned up");
        repository.save(job);
      } catch (Exception e) {
        log.warn("Failed to delete video for job {}: {}", job.getId(), e.getMessage());
      }
    }

    log.info("Cleanup complete: {}/{} video files deleted", deleted, oldJobs.size());
  }
}
