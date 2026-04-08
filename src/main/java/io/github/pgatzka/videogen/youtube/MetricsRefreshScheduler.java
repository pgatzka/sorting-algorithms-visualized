package io.github.pgatzka.videogen.youtube;

import io.github.pgatzka.videogen.job.VideoJobEntity;
import io.github.pgatzka.videogen.job.VideoJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class MetricsRefreshScheduler {

  private final VideoJobRepository repository;
  private final YouTubeUploadService uploadService;

  public MetricsRefreshScheduler(
      VideoJobRepository repository, YouTubeUploadService uploadService) {
    this.repository = repository;
    this.uploadService = uploadService;
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
  @Transactional
  public void refreshMetrics() {
    if (!uploadService.isEnabled()) {
      return;
    }

    List<VideoJobEntity> uploaded =
        repository.findAll().stream()
            .filter(job -> job.getYoutubeVideoId() != null)
            .filter(job -> !"FAILED".equals(job.getYoutubeStatus()))
            .toList();

    if (uploaded.isEmpty()) {
      return;
    }

    log.info("Refreshing YouTube metrics for {} videos", uploaded.size());

    for (VideoJobEntity job : uploaded) {
      try {
        YouTubeUploadService.VideoMetrics metrics =
            uploadService.fetchMetrics(job.getYoutubeVideoId());
        job.setYoutubeViews(metrics.views());
        job.setYoutubeLikes(metrics.likes());
        job.setYoutubeComments(metrics.comments());
        job.setMetricsUpdatedAt(Instant.now());
        repository.save(job);
        log.debug(
            "Job {}: views={}, likes={}, comments={}",
            job.getId(),
            metrics.views(),
            metrics.likes(),
            metrics.comments());
      } catch (Exception e) {
        log.warn("Failed to refresh metrics for job {}: {}", job.getId(), e.getMessage());
      }
    }

    log.info("Metrics refresh complete");
  }
}
