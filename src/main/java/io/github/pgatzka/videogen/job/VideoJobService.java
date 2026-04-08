package io.github.pgatzka.videogen.job;

import io.github.pgatzka.ApplicationProperties;
import io.github.pgatzka.videogen.algorithm.AlgorithmRegistry;
import io.github.pgatzka.videogen.algorithm.SortingAlgorithm;
import io.github.pgatzka.videogen.algorithm.SortingState;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VideoJobService {

  private final VideoJobRepository repository;
  private final AlgorithmRegistry algorithmRegistry;
  private final VisualizationRegistry visualizationRegistry;
  private final ApplicationProperties properties;
  private final FfmpegEncoderFactory encoderFactory;
  private final VideoJobUpdater jobUpdater;
  private final JobOperator asyncJobLauncher;
  private final Job videoGenerationJob;

  public VideoJobService(
      VideoJobRepository repository,
      AlgorithmRegistry algorithmRegistry,
      VisualizationRegistry visualizationRegistry,
      ApplicationProperties properties,
      FfmpegEncoderFactory encoderFactory,
      VideoJobUpdater jobUpdater,
      @Qualifier("asyncJobLauncher") JobOperator asyncJobLauncher,
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
      int framesPerStep,
      boolean shuffle) {
    log.info(
        "Creating video job: algorithm={}, visualization={}, elements={}, fps={}, "
            + "resolution={}x{}, framesPerStep={}, shuffle={}",
        algorithm,
        visualization,
        elementCount,
        fps,
        width,
        height,
        framesPerStep,
        shuffle);

    VideoJobEntity entity = new VideoJobEntity();
    entity.setAlgorithm(algorithm);
    entity.setVisualization(visualization);
    entity.setElementCount(elementCount);
    entity.setFps(fps);
    entity.setWidth(width);
    entity.setHeight(height);
    entity.setFramesPerStep(framesPerStep);
    entity.setShuffle(shuffle);
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
      int framesPerStep,
      boolean shuffle) {
    VideoJobEntity job =
        createJob(
            algorithm, visualization, elementCount, fps, width, height, framesPerStep, shuffle);
    try {
      var params =
          new JobParametersBuilder()
              .addString("jobId", job.getId().toString())
              .addLong("timestamp", System.currentTimeMillis())
              .toJobParameters();
      asyncJobLauncher.start(videoGenerationJob, params);
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
      int paddingFrames = job.getFps(); // 1 second of padding

      log.info(
          "Job {}: Generating states with {} (elements={}, shuffle={})",
          jobId,
          algorithm.getName(),
          job.getElementCount(),
          job.isShuffle());

      List<SortingState> shuffleStates;
      List<SortingState> sortStates;

      if (job.isShuffle()) {
        shuffleStates = generateShuffleStates(job.getElementCount());
        int[] shuffledArray = shuffleStates.getLast().array();
        sortStates = algorithm.sort(shuffledArray);
      } else {
        shuffleStates = List.of();
        int[] shuffledArray = generateRandomArray(job.getElementCount());
        sortStates = algorithm.sort(shuffledArray);
      }

      int animationFrames = (shuffleStates.size() + sortStates.size()) * job.getFramesPerStep();
      int totalPaddingFrames = job.isShuffle() ? 3 * paddingFrames : 2 * paddingFrames;
      int totalFrames = animationFrames + totalPaddingFrames;
      log.info(
          "Job {}: Will render {} total frames ({} shuffle + {} sort states, {} padding)",
          jobId,
          totalFrames,
          shuffleStates.size(),
          sortStates.size(),
          totalPaddingFrames);

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

        if (job.isShuffle()) {
          // Sorted array visible + 1s wait
          Set<Integer> allSorted =
              IntStream.range(0, job.getElementCount())
                  .boxed()
                  .collect(java.util.stream.Collectors.toSet());
          SortingState sortedState =
              new SortingState(
                  IntStream.rangeClosed(1, job.getElementCount()).toArray(),
                  -1,
                  -1,
                  false,
                  allSorted);
          BufferedImage sortedFrame =
              visualization.renderFrame(sortedState, job.getWidth(), job.getHeight());
          for (int f = 0; f < paddingFrames; f++) {
            encoder.writeFrame(sortedFrame);
            framesWritten++;
          }

          // Shuffle animation
          framesWritten =
              writeStates(encoder, visualization, shuffleStates, job, framesWritten, totalFrames);

          // 1s wait after shuffle
          BufferedImage lastShuffleFrame =
              visualization.renderFrame(shuffleStates.getLast(), job.getWidth(), job.getHeight());
          for (int f = 0; f < paddingFrames; f++) {
            encoder.writeFrame(lastShuffleFrame);
            framesWritten++;
          }
        } else {
          // 1s wait before sort
          BufferedImage firstFrame =
              visualization.renderFrame(sortStates.getFirst(), job.getWidth(), job.getHeight());
          for (int f = 0; f < paddingFrames; f++) {
            encoder.writeFrame(firstFrame);
            framesWritten++;
          }
        }

        // Sort animation
        framesWritten =
            writeStates(encoder, visualization, sortStates, job, framesWritten, totalFrames);

        // 1s wait after sort
        BufferedImage lastFrame =
            visualization.renderFrame(sortStates.getLast(), job.getWidth(), job.getHeight());
        for (int f = 0; f < paddingFrames; f++) {
          encoder.writeFrame(lastFrame);
          framesWritten++;
        }

        jobUpdater.updateProgress(jobId, 100);
        log.info("Job {}: Progress 100% ({}/{} frames)", jobId, framesWritten, totalFrames);
      }

      jobUpdater.markCompleted(jobId, outputPath.toAbsolutePath().toString(), Instant.now());
      log.info("Job {}: Completed successfully. Output: {}", jobId, outputPath);

    } catch (Exception e) {
      log.error("Job {}: Failed with error: {}", jobId, e.getMessage(), e);
      jobUpdater.markFailed(jobId, e.getMessage(), Instant.now());
    }
  }

  private int writeStates(
      FfmpegEncoder encoder,
      Visualization visualization,
      List<SortingState> states,
      VideoJobEntity job,
      int framesWritten,
      int totalFrames)
      throws Exception {
    for (SortingState state : states) {
      BufferedImage frame = visualization.renderFrame(state, job.getWidth(), job.getHeight());
      for (int f = 0; f < job.getFramesPerStep(); f++) {
        encoder.writeFrame(frame);
        framesWritten++;
      }
    }
    int percent = (int) ((framesWritten * 100L) / totalFrames);
    jobUpdater.updateProgress(job.getId(), percent);
    return framesWritten;
  }

  @Transactional(readOnly = true)
  public Optional<VideoJobEntity> getJob(UUID id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<VideoJobEntity> getAllJobs() {
    return repository.findAllByOrderByCreatedAtDesc();
  }

  private List<SortingState> generateShuffleStates(int size) {
    int[] array = IntStream.rangeClosed(1, size).toArray();
    List<SortingState> states = new ArrayList<>();
    Random random = new Random();

    for (int i = array.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      if (i != j) {
        states.add(new SortingState(array, i, j, false, Set.of()));
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
        states.add(new SortingState(array, i, j, true, Set.of()));
      }
    }

    states.add(new SortingState(array, -1, -1, false, Set.of()));
    return states;
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
