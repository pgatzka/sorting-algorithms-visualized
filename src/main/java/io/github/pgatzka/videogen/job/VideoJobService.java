package io.github.pgatzka.videogen.job;

import io.github.pgatzka.ApplicationProperties;
import io.github.pgatzka.videogen.algorithm.AlgorithmRegistry;
import io.github.pgatzka.videogen.algorithm.SortingAlgorithm;
import io.github.pgatzka.videogen.algorithm.SortingState;
import io.github.pgatzka.videogen.encoding.AudioGenerator;
import io.github.pgatzka.videogen.encoding.FfmpegEncoder;
import io.github.pgatzka.videogen.encoding.FfmpegEncoderFactory;
import io.github.pgatzka.videogen.visualization.ColorScheme;
import io.github.pgatzka.videogen.visualization.FramePostProcessor;
import io.github.pgatzka.videogen.visualization.PollIntroRenderer;
import io.github.pgatzka.videogen.visualization.SideBySideRenderer;
import io.github.pgatzka.videogen.visualization.StatsOverlay;
import io.github.pgatzka.videogen.visualization.TitleOverlay;
import io.github.pgatzka.videogen.visualization.TweeningRenderer;
import io.github.pgatzka.videogen.visualization.Visualization;
import io.github.pgatzka.videogen.visualization.VisualizationRegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
  public VideoJobEntity createJob(VideoJobRequest request) {
    log.info("Creating video job: {}", request);

    VideoJobEntity entity = new VideoJobEntity();
    entity.setAlgorithm(request.getAlgorithm());
    entity.setVisualization(request.getVisualization());
    entity.setElementCount(request.getElementCount());
    entity.setFps(request.getFps());
    entity.setWidth(request.getWidth());
    entity.setHeight(request.getHeight());
    entity.setFramesPerStep(request.getFramesPerStep());
    entity.setShuffle(request.isShuffle());
    entity.setColorScheme(request.getColorScheme());
    entity.setSound(request.isSound());
    entity.setShowStats(request.isShowStats());
    entity.setGlowEffect(request.isGlowEffect());
    entity.setParticleTrail(request.isParticleTrail());
    entity.setTweening(request.isTweening());
    entity.setSpeedRun(request.isSpeedRun());
    entity.setSecondAlgorithm(request.getSecondAlgorithm());
    entity.setDebug(request.isDebug());
    entity.setStatus(VideoJobStatus.QUEUED);
    entity.setProgress(0);

    VideoJobEntity saved = repository.save(entity);
    log.info("Video job created with id={}", saved.getId());
    return saved;
  }

  public VideoJobEntity submitJob(VideoJobRequest request) {
    VideoJobEntity job = createJob(request);
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
      boolean isSideBySide =
          job.getSecondAlgorithm() != null && !job.getSecondAlgorithm().isBlank();
      if (isSideBySide) {
        executeSideBySideJob(job);
        return;
      }

      SortingAlgorithm algorithm = algorithmRegistry.getByName(job.getAlgorithm());
      Visualization visualization = visualizationRegistry.getByName(job.getVisualization());
      ColorScheme colorScheme = ColorScheme.valueOf(job.getColorScheme());
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

      // Victory sweep: highlight each element left to right
      List<SortingState> sweepStates = new ArrayList<>();
      int[] sortedArray = sortStates.getLast().array();
      for (int i = 0; i < job.getElementCount(); i++) {
        sweepStates.add(new SortingState(sortedArray, i, -1, false, Set.of()));
      }

      int animationFrames =
          (shuffleStates.size() + sortStates.size() + sweepStates.size()) * job.getFramesPerStep();
      int totalPaddingFrames = job.isShuffle() ? 3 * paddingFrames : 2 * paddingFrames;
      int totalFrames = animationFrames + totalPaddingFrames;
      log.info(
          "Job {}: Will render {} total frames ({} shuffle + {} sort + {} sweep states, {} padding)",
          jobId,
          totalFrames,
          shuffleStates.size(),
          sortStates.size(),
          sweepStates.size(),
          totalPaddingFrames);

      Path outputDir = Path.of(properties.getOutputDir());
      Files.createDirectories(outputDir);

      String filename = buildFilename(job);
      Path outputPath = outputDir.resolve(filename);

      Path videoPath = job.isSound() ? outputPath.resolveSibling("tmp_" + filename) : outputPath;
      log.info("Job {}: Encoding video to {}", jobId, videoPath);

      // Apply speed run overrides
      int effectiveFps = job.isSpeedRun() ? 60 : job.getFps();
      int effectiveFramesPerStep = job.isSpeedRun() ? 1 : job.getFramesPerStep();

      List<String> debugLines =
          job.isDebug() ? buildDebugLines(job, effectiveFps, effectiveFramesPerStep) : null;
      StatsOverlay statsOverlay =
          job.isShowStats()
              ? new StatsOverlay(job.getElementCount(), new Random().nextInt(10), debugLines)
              : null;

      FramePostProcessor postProcessor =
          new FramePostProcessor(
              job.getAlgorithm(),
              statsOverlay,
              job.isGlowEffect(),
              job.isParticleTrail(),
              job.getElementCount());

      TweeningRenderer tweener = job.isTweening() ? new TweeningRenderer() : null;
      int[] counters = new int[2];

      try (FfmpegEncoder encoder = encoderFactory.create()) {
        encoder.start(
            videoPath, job.getWidth(), job.getHeight(), effectiveFps, properties.getFfmpegPath());

        int framesWritten = 0;

        // Poll intro (2s)
        if (job.getSecondAlgorithm() != null && !job.getSecondAlgorithm().isBlank()) {
          BufferedImage introFrame =
              new PollIntroRenderer()
                  .renderVersus(
                      job.getAlgorithm(),
                      job.getSecondAlgorithm(),
                      job.getElementCount(),
                      job.getWidth(),
                      job.getHeight());
          for (int f = 0; f < effectiveFps * 2; f++) {
            encoder.writeFrame(introFrame);
            framesWritten++;
          }
        }

        if (job.isShuffle()) {
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
          framesWritten =
              writePadding(
                  encoder,
                  visualization,
                  colorScheme,
                  sortedState,
                  job,
                  postProcessor,
                  framesWritten,
                  paddingFrames,
                  counters,
                  effectiveFps);

          framesWritten =
              writeStates(
                  encoder,
                  visualization,
                  colorScheme,
                  shuffleStates,
                  job,
                  postProcessor,
                  tweener,
                  framesWritten,
                  totalFrames,
                  counters,
                  false,
                  effectiveFramesPerStep,
                  effectiveFps);

          framesWritten =
              writePadding(
                  encoder,
                  visualization,
                  colorScheme,
                  shuffleStates.getLast(),
                  job,
                  postProcessor,
                  framesWritten,
                  paddingFrames,
                  counters,
                  effectiveFps);
        } else {
          framesWritten =
              writePadding(
                  encoder,
                  visualization,
                  colorScheme,
                  sortStates.getFirst(),
                  job,
                  postProcessor,
                  framesWritten,
                  paddingFrames,
                  counters,
                  effectiveFps);
        }

        if (statsOverlay != null) {
          statsOverlay.startCounting(framesWritten);
        }

        framesWritten =
            writeStates(
                encoder,
                visualization,
                colorScheme,
                sortStates,
                job,
                postProcessor,
                tweener,
                framesWritten,
                totalFrames,
                counters,
                true,
                effectiveFramesPerStep,
                effectiveFps);

        if (statsOverlay != null) {
          statsOverlay.freeze(framesWritten);
        }

        framesWritten =
            writeStates(
                encoder,
                visualization,
                colorScheme,
                sweepStates,
                job,
                postProcessor,
                tweener,
                framesWritten,
                totalFrames,
                counters,
                false,
                effectiveFramesPerStep,
                effectiveFps);

        framesWritten =
            writePadding(
                encoder,
                visualization,
                colorScheme,
                sortStates.getLast(),
                job,
                postProcessor,
                framesWritten,
                paddingFrames,
                counters,
                effectiveFps);

        jobUpdater.updateProgress(jobId, 100);
        log.info("Job {}: Progress 100% ({}/{} frames)", jobId, framesWritten, totalFrames);
      }

      if (job.isSound()) {
        Path audioPath = outputPath.resolveSibling("tmp_" + filename.replace(".mp4", ".wav"));
        try {
          List<AudioGenerator.AudioPhase> audioPhases = new ArrayList<>();
          if (job.isShuffle()) {
            audioPhases.add(new AudioGenerator.AudioPhase(paddingFrames, shuffleStates));
            audioPhases.add(new AudioGenerator.AudioPhase(paddingFrames, sortStates));
          } else {
            audioPhases.add(new AudioGenerator.AudioPhase(paddingFrames, sortStates));
          }
          audioPhases.add(new AudioGenerator.AudioPhase(0, sweepStates));

          new AudioGenerator()
              .generate(
                  audioPath,
                  audioPhases,
                  effectiveFramesPerStep,
                  effectiveFps,
                  job.getElementCount(),
                  paddingFrames);

          muxAudioVideo(videoPath, audioPath, outputPath, properties.getFfmpegPath());
        } finally {
          Files.deleteIfExists(videoPath);
          Files.deleteIfExists(audioPath);
        }
      }

      jobUpdater.markCompleted(jobId, outputPath.toAbsolutePath().toString(), Instant.now());
      log.info("Job {}: Completed successfully. Output: {}", jobId, outputPath);

    } catch (Exception e) {
      log.error("Job {}: Failed with error: {}", jobId, e.getMessage(), e);
      jobUpdater.markFailed(jobId, e.getMessage(), Instant.now());
    }
  }

  private void executeSideBySideJob(VideoJobEntity job) throws Exception {
    UUID jobId = job.getId();
    SortingAlgorithm algo1 = algorithmRegistry.getByName(job.getAlgorithm());
    SortingAlgorithm algo2 = algorithmRegistry.getByName(job.getSecondAlgorithm());
    Visualization visualization = visualizationRegistry.getByName(job.getVisualization());
    ColorScheme colorScheme = ColorScheme.valueOf(job.getColorScheme());
    int effectiveFps = job.isSpeedRun() ? 60 : job.getFps();
    int effectiveFramesPerStep = job.isSpeedRun() ? 1 : job.getFramesPerStep();
    int paddingFrames = effectiveFps;

    int[] inputArray = generateRandomArray(job.getElementCount());
    List<SortingState> states1 = algo1.sort(inputArray.clone());
    List<SortingState> states2 = algo2.sort(inputArray.clone());

    // Pad shorter list to match longer
    int maxLen = Math.max(states1.size(), states2.size());
    while (states1.size() < maxLen) states1.add(states1.getLast());
    while (states2.size() < maxLen) states2.add(states2.getLast());

    int totalFrames = maxLen * effectiveFramesPerStep + paddingFrames * 2;

    Path outputDir = Path.of(properties.getOutputDir());
    Files.createDirectories(outputDir);
    String filename = buildFilename(job);
    Path outputPath = outputDir.resolve(filename);
    Path videoPath = job.isSound() ? outputPath.resolveSibling("tmp_" + filename) : outputPath;

    SideBySideRenderer sideBySide = new SideBySideRenderer();
    TitleOverlay leftTitle = new TitleOverlay(job.getAlgorithm());
    TitleOverlay rightTitle = new TitleOverlay(job.getSecondAlgorithm());

    try (FfmpegEncoder encoder = encoderFactory.create()) {
      encoder.start(
          videoPath, job.getWidth(), job.getHeight(), effectiveFps, properties.getFfmpegPath());

      int framesWritten = 0;

      // Poll intro (2s)
      BufferedImage introFrame =
          new PollIntroRenderer()
              .renderVersus(
                  job.getAlgorithm(),
                  job.getSecondAlgorithm(),
                  job.getElementCount(),
                  job.getWidth(),
                  job.getHeight());
      for (int f = 0; f < effectiveFps * 2; f++) {
        encoder.writeFrame(introFrame);
        framesWritten++;
      }

      // 1s padding
      BufferedImage firstFrame =
          sideBySide.render(
              visualization,
              states1.getFirst(),
              states2.getFirst(),
              job.getWidth(),
              job.getHeight(),
              colorScheme);
      for (int f = 0; f < paddingFrames; f++) {
        encoder.writeFrame(firstFrame);
        framesWritten++;
      }

      // Sort animation
      for (int i = 0; i < maxLen; i++) {
        BufferedImage frame =
            sideBySide.render(
                visualization,
                states1.get(i),
                states2.get(i),
                job.getWidth(),
                job.getHeight(),
                colorScheme);

        // Overlay algorithm names on each half
        var g = frame.createGraphics();
        g.dispose();

        for (int f = 0; f < effectiveFramesPerStep; f++) {
          encoder.writeFrame(frame);
          framesWritten++;
        }
      }

      // 1s padding
      BufferedImage lastFrame =
          sideBySide.render(
              visualization,
              states1.getLast(),
              states2.getLast(),
              job.getWidth(),
              job.getHeight(),
              colorScheme);
      for (int f = 0; f < paddingFrames; f++) {
        encoder.writeFrame(lastFrame);
        framesWritten++;
      }

      jobUpdater.updateProgress(jobId, 100);
    }

    if (job.isSound()) {
      Path audioPath = outputPath.resolveSibling("tmp_" + filename.replace(".mp4", ".wav"));
      try {
        List<AudioGenerator.AudioPhase> audioPhases = new ArrayList<>();
        audioPhases.add(new AudioGenerator.AudioPhase(effectiveFps * 2 + paddingFrames, states1));
        new AudioGenerator()
            .generate(
                audioPath,
                audioPhases,
                effectiveFramesPerStep,
                effectiveFps,
                job.getElementCount(),
                paddingFrames);
        muxAudioVideo(videoPath, audioPath, outputPath, properties.getFfmpegPath());
      } finally {
        Files.deleteIfExists(videoPath);
        Files.deleteIfExists(audioPath);
      }
    }

    jobUpdater.markCompleted(jobId, outputPath.toAbsolutePath().toString(), Instant.now());
    log.info("Job {}: Side-by-side completed. Output: {}", jobId, outputPath);
  }

  private int writeStates(
      FfmpegEncoder encoder,
      Visualization visualization,
      ColorScheme colorScheme,
      List<SortingState> states,
      VideoJobEntity job,
      FramePostProcessor postProcessor,
      TweeningRenderer tweener,
      int framesWritten,
      int totalFrames,
      int[] counters,
      boolean countStats,
      int framesPerStep,
      int fps)
      throws Exception {
    SortingState previousState = null;
    for (SortingState state : states) {
      if (countStats && state.compareIdx1() >= 0 && state.compareIdx2() >= 0) {
        counters[0] += 2;
        counters[1]++;
        if (state.swapped()) {
          counters[0] += 2;
        }
      }

      if (tweener != null && previousState != null && framesPerStep > 1) {
        for (int f = 0; f < framesPerStep; f++) {
          float t = (float) (f + 1) / framesPerStep;
          BufferedImage frame =
              tweener.renderInterpolated(
                  visualization,
                  previousState,
                  state,
                  t,
                  job.getWidth(),
                  job.getHeight(),
                  colorScheme);
          postProcessor.process(frame, state, framesWritten, fps, counters);
          encoder.writeFrame(frame);
          framesWritten++;
        }
      } else {
        BufferedImage frame =
            visualization.renderFrame(state, job.getWidth(), job.getHeight(), colorScheme);
        postProcessor.process(frame, state, framesWritten, fps, counters);
        for (int f = 0; f < framesPerStep; f++) {
          encoder.writeFrame(frame);
          framesWritten++;
        }
      }
      previousState = state;
    }
    int percent = (int) ((framesWritten * 100L) / totalFrames);
    jobUpdater.updateProgress(job.getId(), percent);
    return framesWritten;
  }

  private int writePadding(
      FfmpegEncoder encoder,
      Visualization visualization,
      ColorScheme colorScheme,
      SortingState state,
      VideoJobEntity job,
      FramePostProcessor postProcessor,
      int framesWritten,
      int paddingFrames,
      int[] counters,
      int fps)
      throws Exception {
    for (int f = 0; f < paddingFrames; f++) {
      BufferedImage frame =
          visualization.renderFrame(state, job.getWidth(), job.getHeight(), colorScheme);
      postProcessor.process(frame, state, framesWritten, fps, counters);
      encoder.writeFrame(frame);
      framesWritten++;
    }
    return framesWritten;
  }

  private String buildFilename(VideoJobEntity job) {
    StringBuilder name = new StringBuilder();
    name.append(job.getAlgorithm());
    if (job.getSecondAlgorithm() != null && !job.getSecondAlgorithm().isBlank()) {
      name.append("_vs_").append(job.getSecondAlgorithm());
    }
    name.append("_").append(job.getVisualization());
    name.append("_").append(job.getColorScheme());
    List<String> flags = new ArrayList<>();
    if (job.isShuffle()) flags.add("shuffle");
    if (job.isSound()) flags.add("sound");
    if (job.isGlowEffect()) flags.add("glow");
    if (job.isParticleTrail()) flags.add("particles");
    if (job.isTweening()) flags.add("tween");
    if (job.isSpeedRun()) flags.add("speedrun");
    if (!flags.isEmpty()) {
      name.append("_").append(String.join("-", flags));
    }
    name.append("_").append(job.getElementCount()).append("el");
    name.append("_")
        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    name.append(".mp4");
    return name.toString().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
  }

  private List<String> buildDebugLines(
      VideoJobEntity job, int effectiveFps, int effectiveFramesPerStep) {
    List<String> lines = new ArrayList<>();
    lines.add(String.format("Algorithm: %s", job.getAlgorithm()));
    lines.add(String.format("Visualization: %s", job.getVisualization()));
    lines.add(String.format("Resolution: %dx%d", job.getWidth(), job.getHeight()));
    lines.add(String.format("FPS: %d (effective: %d)", job.getFps(), effectiveFps));
    lines.add(
        String.format(
            "Frames/Step: %d (effective: %d)", job.getFramesPerStep(), effectiveFramesPerStep));
    lines.add(String.format("Color: %s", job.getColorScheme()));
    StringBuilder flags = new StringBuilder();
    if (job.isShuffle()) flags.append("shuffle ");
    if (job.isSound()) flags.append("sound ");
    if (job.isGlowEffect()) flags.append("glow ");
    if (job.isParticleTrail()) flags.append("particles ");
    if (job.isTweening()) flags.append("tweening ");
    if (job.isSpeedRun()) flags.append("speedrun ");
    if (job.isShowStats()) flags.append("stats ");
    if (job.isDebug()) flags.append("debug ");
    lines.add(String.format("Flags: %s", flags.toString().trim()));
    if (job.getSecondAlgorithm() != null && !job.getSecondAlgorithm().isBlank()) {
      lines.add(String.format("VS: %s", job.getSecondAlgorithm()));
    }
    return lines;
  }

  private void muxAudioVideo(Path videoPath, Path audioPath, Path outputPath, String ffmpegPath)
      throws IOException, InterruptedException {
    log.info("Muxing audio and video: {} + {} -> {}", videoPath, audioPath, outputPath);
    ProcessBuilder pb =
        new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-i",
            videoPath.toAbsolutePath().toString(),
            "-i",
            audioPath.toAbsolutePath().toString(),
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-b:a",
            "192k",
            "-shortest",
            "-movflags",
            "+faststart",
            outputPath.toAbsolutePath().toString());
    pb.redirectErrorStream(true);
    Process process = pb.start();

    try (var reader =
        new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        log.debug("FFmpeg mux: {}", line);
      }
    }

    boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
    if (!exited) {
      process.destroyForcibly();
      throw new IOException("FFmpeg mux process timed out");
    }
    int exitCode = process.exitValue();
    if (exitCode != 0) {
      throw new IOException("FFmpeg mux exited with code " + exitCode);
    }
    log.info("Audio/video mux completed: {}", outputPath);
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
