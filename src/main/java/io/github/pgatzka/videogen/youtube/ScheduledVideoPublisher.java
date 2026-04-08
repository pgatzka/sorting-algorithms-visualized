package io.github.pgatzka.videogen.youtube;

import io.github.pgatzka.ApplicationProperties;
import io.github.pgatzka.videogen.algorithm.AlgorithmRegistry;
import io.github.pgatzka.videogen.job.*;
import io.github.pgatzka.videogen.visualization.VisualizationRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduledVideoPublisher {

  private static final String[] COLOR_SCHEMES = {"DEFAULT", "RAINBOW", "GREYSCALE"};

  private final VideoJobService jobService;
  private final YouTubeUploadService uploadService;
  private final VideoJobUpdater jobUpdater;
  private final AlgorithmRegistry algorithmRegistry;
  private final VisualizationRegistry visualizationRegistry;
  private final ApplicationProperties appProperties;
  private final CaptionGenerator captionGenerator = new CaptionGenerator();
  private final Random random = new Random();

  public ScheduledVideoPublisher(
      VideoJobService jobService,
      YouTubeUploadService uploadService,
      VideoJobUpdater jobUpdater,
      AlgorithmRegistry algorithmRegistry,
      VisualizationRegistry visualizationRegistry,
      ApplicationProperties appProperties) {
    this.jobService = jobService;
    this.uploadService = uploadService;
    this.jobUpdater = jobUpdater;
    this.algorithmRegistry = algorithmRegistry;
    this.visualizationRegistry = visualizationRegistry;
    this.appProperties = appProperties;
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  public void generateAndUpload() {
    if (!uploadService.isEnabled()) {
      log.debug("YouTube upload disabled, skipping scheduled generation");
      return;
    }

    try {
      log.info("Scheduled YouTube video generation starting...");
      VideoJobRequest request = buildRandomRequest();
      VideoJobEntity job = jobService.submitJob(request);
      log.info("Scheduled job submitted: id={} (autoUpload=true)", job.getId());

      // Wait for full completion including upload (max 15 minutes)
      for (int i = 0; i < 180; i++) {
        Thread.sleep(5000);
        VideoJobEntity check = jobService.getJob(job.getId()).orElseThrow();
        if (check.getStatus() == VideoJobStatus.COMPLETED
            || check.getStatus() == VideoJobStatus.FAILED) {
          break;
        }
      }

      VideoJobEntity completed = jobService.getJob(job.getId()).orElseThrow();
      if (completed.getStatus() == VideoJobStatus.COMPLETED) {
        log.info("Scheduled job completed: id={}, youtube={}", job.getId(), completed.getYoutubeVideoId());
      } else {
        log.error(
            "Scheduled job failed: id={}, error={}",
            job.getId(),
            completed.getErrorMessage());
      }

    } catch (Exception e) {
      log.error("Scheduled YouTube generation/upload failed", e);
    }
  }

  public VideoJobRequest buildRandomRequest() {
    List<String> algorithms = algorithmRegistry.getAllNames();
    List<String> visualizations = visualizationRegistry.getAllNames();

    List<String> safeAlgorithms =
        algorithms.stream()
            .filter(
                name ->
                    !name.equals("BogoSort")
                        && !name.equals("StoogeSort")
                        && !name.equals("SlowSort"))
            .toList();

    String algo = safeAlgorithms.get(random.nextInt(safeAlgorithms.size()));
    String viz = visualizations.get(random.nextInt(visualizations.size()));
    String colorScheme = COLOR_SCHEMES[random.nextInt(COLOR_SCHEMES.length)];
    int elementCount = 20 + random.nextInt(81);

    String secondAlgo = null;
    if (random.nextInt(10) < 3) {
      List<String> others = safeAlgorithms.stream().filter(a -> !a.equals(algo)).toList();
      secondAlgo = others.get(random.nextInt(others.size()));
    }

    return VideoJobRequest.builder()
        .algorithm(algo)
        .visualization(viz)
        .elementCount(elementCount)
        .fps(appProperties.getDefaultFps())
        .width(appProperties.getDefaultWidth())
        .height(appProperties.getDefaultHeight())
        .framesPerStep(2 + random.nextInt(3))
        .shuffle(true)
        .colorScheme(colorScheme)
        .sound(true)
        .showStats(random.nextBoolean())
        .glowEffect(random.nextBoolean())
        .particleTrail(random.nextInt(10) < 3)
        .tweening(random.nextBoolean())
        .speedRun(random.nextInt(10) < 2)
        .secondAlgorithm(secondAlgo)
        .autoUpload(true)
        .build();
  }
}
