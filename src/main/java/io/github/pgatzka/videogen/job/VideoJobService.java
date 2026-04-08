package io.github.pgatzka.videogen.job;

import io.github.pgatzka.videogen.algorithm.AlgorithmRegistry;
import io.github.pgatzka.videogen.algorithm.SortingAlgorithm;
import io.github.pgatzka.videogen.algorithm.SortingState;
import io.github.pgatzka.videogen.config.VideoGenProperties;
import io.github.pgatzka.videogen.encoding.FfmpegEncoder;
import io.github.pgatzka.videogen.encoding.FfmpegEncoderFactory;
import io.github.pgatzka.videogen.visualization.Visualization;
import io.github.pgatzka.videogen.visualization.VisualizationRegistry;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VideoJobService {

  private final VideoJobRepository repository;
  private final AlgorithmRegistry algorithmRegistry;
  private final VisualizationRegistry visualizationRegistry;
  private final VideoGenProperties properties;
  private final FfmpegEncoderFactory encoderFactory;
  private final VideoJobUpdater jobUpdater;
  private final JobLauncher asyncJobLauncher;
  private final Job videoGenerationJob;

  public VideoJobService(
      VideoJobRepository repository,
      AlgorithmRegistry algorithmRegistry,
      VisualizationRegistry visualizationRegistry,
      VideoGenProperties properties,
      FfmpegEncoderFactory encoderFactory,
      VideoJobUpdater jobUpdater,
      @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
      Job videoGenerationJob) {
    this.repository = repository;
    this.algorithmRegistry = algorithmRegistry;
    this.visualizationRegistry = visualizationRegistry;
    this.properties = properties;
    this.encoderFactory = encoderFactory;
    this.jobUpdater = jobUpdater;
    this.asyncJobLauncher = asyncJobLauncher;
    this.videoGenerationJob = videoGenerationJob;
  }

  @Transactional
  public VideoJobEntity createJob(
      String algorithm,
      String visualization,
      int elementCount,
      int fps,
      int width,
      int height,
      int framesPerStep) {
    log.info(
        "Creating video job: algorithm={}, visualization={}, elements={}, fps={}, "
            + "resolution={}x{}, framesPerStep={}",
        algorithm,
        visualization,
        elementCount,
        fps,
        width,
        height,
        framesPerStep);

    VideoJobEntity entity = new VideoJobEntity();
    entity.setAlgorithm(algorithm);
    entity.setVisualization(visualization);
    entity.setElementCount(elementCount);
    entity.setFps(fps);
    entity.setWidth(width);
    entity.setHeight(height);
    entity.setFramesPerStep(framesPerStep);
    entity.setStatus(VideoJobStatus.QUEUED);
    entity.setProgress(0);

    VideoJobEntity saved = repository.save(entity);
    log.info("Video job created with id={}", saved.getId());
    return saved;
  }

  public VideoJobEntity submitJob(
      String algorithm,
      String visualization,
      int elementCount,
      int fps,
      int width,
      int height,
      int framesPerStep) {
    VideoJobEntity job =
        createJob(algorithm, visualization, elementCount, fps, width, height, framesPerStep);
    try {
      var params =
          new JobParametersBuilder()
              .addString("jobId", job.getId().toString())
              .addLong("timestamp", System.currentTimeMillis())
              .toJobParameters();
      asyncJobLauncher.run(videoGenerationJob, params);
      log.info("Batch job launched for video job id={}", job.getId());
    } catch (Exception e) {
      log.error("Failed to launch batch job for video job id={}", job.getId(), e);
      job.setStatus(VideoJobStatus.FAILED);
      job.setErrorMessage("Failed to launch batch job: " + e.getMessage());
      repository.save(job);
    }
    return job;
  }

  public void executeJob(UUID jobId) {
    log.info("Starting execution of video job id={}", jobId);

    VideoJobEntity job =
        repository
            .findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

    jobUpdater.markRunning(jobId, Instant.now());

    try {
      SortingAlgorithm algorithm = algorithmRegistry.getByName(job.getAlgorithm());
      Visualization visualization = visualizationRegistry.getByName(job.getVisualization());

      log.info(
          "Job {}: Generating sorting states with {} (elements={})",
          jobId,
          algorithm.getName(),
          job.getElementCount());

      int[] inputArray = generateRandomArray(job.getElementCount());
      List<SortingState> states = algorithm.sort(inputArray);
      log.info("Job {}: Generated {} sorting states", jobId, states.size());

      int totalFrames = states.size() * job.getFramesPerStep();
      log.info(
          "Job {}: Will render {} total frames ({} states x {} frames/step)",
          jobId,
          totalFrames,
          states.size(),
          job.getFramesPerStep());

      Path outputDir = Path.of(properties.getOutputDir());
      Files.createDirectories(outputDir);

      String filename =
          String.format(
              "%s_%s.mp4",
              job.getAlgorithm().toLowerCase(),
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
      Path outputPath = outputDir.resolve(filename);

      log.info("Job {}: Encoding video to {}", jobId, outputPath);

      try (FfmpegEncoder encoder = encoderFactory.create()) {
        encoder.start(
            outputPath, job.getWidth(), job.getHeight(), job.getFps(), properties.getFfmpegPath());

        int framesWritten = 0;
        int lastLoggedPercent = 0;

        for (int stateIdx = 0; stateIdx < states.size(); stateIdx++) {
          SortingState state = states.get(stateIdx);
          BufferedImage frame = visualization.renderFrame(state, job.getWidth(), job.getHeight());

          for (int f = 0; f < job.getFramesPerStep(); f++) {
            encoder.writeFrame(frame);
            framesWritten++;
          }

          int percent = (int) ((framesWritten * 100L) / totalFrames);
          if (percent >= lastLoggedPercent + 10) {
            lastLoggedPercent = (percent / 10) * 10;
            log.info(
                "Job {}: Progress {}% ({}/{} frames)",
                jobId, lastLoggedPercent, framesWritten, totalFrames);
            jobUpdater.updateProgress(jobId, percent);
          }
        }
      }

      jobUpdater.markCompleted(jobId, outputPath.toAbsolutePath().toString(), Instant.now());
      log.info("Job {}: Completed successfully. Output: {}", jobId, outputPath);

    } catch (Exception e) {
      log.error("Job {}: Failed with error: {}", jobId, e.getMessage(), e);
      jobUpdater.markFailed(jobId, e.getMessage(), Instant.now());
    }
  }

  @Transactional(readOnly = true)
  public Optional<VideoJobEntity> getJob(UUID id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<VideoJobEntity> getAllJobs() {
    return repository.findAllByOrderByCreatedAtDesc();
  }

  private int[] generateRandomArray(int size) {
    int[] array = IntStream.rangeClosed(1, size).toArray();
    Random random = new Random();
    for (int i = array.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
    return array;
  }
}
