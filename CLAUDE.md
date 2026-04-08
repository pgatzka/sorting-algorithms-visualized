# Sorting Algorithms Visualized

Spring Boot 4.0.5 + Vaadin 25.1.1 (Flow) application that generates sorting algorithm visualization videos with sound, visual effects, and automatic YouTube uploads.

## Tech Stack

- **Java 21**, **Spring Boot 4.0.5**, **Vaadin 25.1.1** (server-side Flow model)
- **JPA/Hibernate** with PostgreSQL (runtime) and H2 (tests)
- **Flyway** for database migrations (`src/main/resources/db/migration/`)
- **Spring Batch 6** for async video generation jobs (`JobOperator`, not the deprecated `JobLauncher`)
- **Lombok** for boilerplate reduction
- **Lumo** theme with dark mode, compact stylesheet, utility stylesheet
- **FFmpeg** for video encoding and audio muxing (external binary)

## Common Commands

```bash
./mvnw                      # Default goal: spring-boot:run (activates quiet + dev profiles)
./mvnw verify -B            # Build, test, spotless check
./mvnw spotless:check       # Check formatting
./mvnw spotless:apply       # Auto-format code
./mvnw sonar:sonar          # Run SonarCloud analysis (requires SONAR_TOKEN)
docker compose up -d --build # Run with PostgreSQL via Docker
```

## Project Structure

```
src/main/java/io/github/pgatzka/
  Application.java                  # Entry point, @EnableScheduling, @EnableConfigurationProperties
  ApplicationProperties.java        # videogen.* config properties (root package)

src/main/java/io/github/pgatzka/videogen/
  algorithm/                        # Sorting algorithms (all implement SortingAlgorithm)
    SortingAlgorithm.java           # Interface: getName() + sort(int[]) -> List<SortingState>
    SortingState.java               # Record: array snapshot + compareIdx1/2 + swapped + sorted set
    AlgorithmRegistry.java          # Spring-managed registry, auto-discovers @Component algorithms
    BubbleSortAlgorithm.java        # 17 algorithms total (see full list below)
    ...
  visualization/                    # Video frame renderers (all implement Visualization)
    Visualization.java              # Interface: getName() + renderFrame(state, w, h, colorScheme)
    VisualizationRegistry.java      # Auto-discovers @Component visualizations
    ColorScheme.java                # Enum: DEFAULT, RAINBOW, GREYSCALE
    BarChartVisualization.java      # 10 visualizations total (see full list below)
    CircleVisualization.java
    ...
    StatsOverlay.java               # Stats HUD: elements, duration, comparisons, array accesses, fun fact
    TitleOverlay.java               # Algorithm name at bottom of frame (always on)
    GlowEffect.java                 # Bloom effect on compare/swap pixels (scans rendered frame)
    ParticleTrailEffect.java        # Particle system spawned at swap-colored pixels
    TweeningRenderer.java           # Interpolates array values between states
    FramePostProcessor.java         # Bundles all per-frame effects (glow, particles, stats, title)
    PollIntroRenderer.java          # "Who wins?" intro screen for side-by-side
    SideBySideRenderer.java         # Split-screen renderer for two algorithms
  encoding/
    FfmpegEncoder.java              # Pipes raw BGR frames to FFmpeg stdin
    FfmpegEncoderFactory.java       # Creates FfmpegEncoder instances (mockable in tests)
    AudioGenerator.java             # Generates WAV with sine tones mapped to values
  job/
    VideoJobEntity.java             # JPA entity with all job config + status + YouTube fields
    VideoJobRequest.java            # Builder DTO for creating jobs
    VideoJobStatus.java             # Enum: QUEUED, RUNNING, UPLOADING, COMPLETED, FAILED
    VideoJobService.java            # Core orchestrator: executeJob() and executeSideBySideJob()
    VideoJobUpdater.java            # Transactional status/progress updates (REQUIRES_NEW)
    VideoJobRepository.java         # Spring Data JPA repository
    VideoGenerationTasklet.java     # Spring Batch tasklet that calls VideoJobService.executeJob()
    VideoCleanupScheduler.java      # Hourly cleanup of old video files (configurable retention)
  config/
    VideoGenConfiguration.java      # Spring Batch job/step/operator beans
  youtube/
    YouTubeProperties.java          # youtube.* config properties
    YouTubeUploadService.java       # Upload via YouTube Data API v3 (resumable upload)
    YouTubeDeviceAuthService.java   # OAuth2 device flow (no redirect URI needed)
    YouTubeTokenStore.java          # Persists tokens to file (survives restarts)
    CaptionGenerator.java           # Generates titles + descriptions with hashtags
    ScheduledVideoPublisher.java    # @Scheduled every 4h: random video + upload
    MetricsRefreshScheduler.java    # @Scheduled every 1h: fetches YouTube view/like counts
  ui/
    VideoGeneratorView.java         # Main UI at / — form, grid, batch button, YouTube auth
    AnalyticsDashboardView.java     # Dashboard at /analytics — metrics, rankings, refresh button
    TermsOfServiceView.java         # /terms
    PrivacyPolicyView.java          # /privacy

src/main/resources/
  application.yml                   # Prod config (env vars for secrets, YouTube config)
  application-quiet.yml             # Banner off, atmosphere logging reduced
  application-dev.yml               # Dev DB credentials, ddl-auto: update
  db/migration/                     # Flyway V1-V12 migrations
  vaadin-i18n/translations.properties  # All UI labels (i18n ready)

src/test/resources/
  application.yml                   # H2 in-memory DB, Flyway disabled, ddl-auto: create-drop
```

## Architecture

### Video Generation Pipeline

```
submitJob(VideoJobRequest)
  → createJob() persists VideoJobEntity (QUEUED)
  → Spring Batch async launcher starts VideoGenerationTasklet
    → executeJob(jobId):
        1. Mark RUNNING
        2. Generate sort states via SortingAlgorithm.sort()
        3. Optionally generate shuffle states (Fisher-Yates with state capture)
        4. Generate victory sweep states
        5. Encode video:
           a. Poll intro (if side-by-side)
           b. Padding (1s) → Shuffle animation → Padding (1s)
           c. Sort animation (stats counting starts here)
           d. Victory sweep (stats frozen)
           e. Padding (1s)
        6. If sound enabled: generate WAV → FFmpeg mux audio+video
        7. If autoUpload: upload to YouTube → wait for public
        8. Mark COMPLETED
```

### Frame Rendering Pipeline

```
For each SortingState:
  1. Visualization.renderFrame() → BufferedImage (TYPE_3BYTE_BGR)
  2. If tweening: TweeningRenderer interpolates between previous and current state
  3. FramePostProcessor.process():
     a. GlowEffect.apply() — scans frame for compare/swap colors, draws ARGB bloom overlay
     b. ParticleTrailEffect.update() + render() — spawns particles at swap pixels, ARGB overlay
     c. StatsOverlay.render() — stats HUD with debug lines if enabled
     d. TitleOverlay.render() — algorithm name at bottom
  4. FfmpegEncoder.writeFrame() × framesPerStep
```

### Side-by-Side Mode

When `secondAlgorithm` is set, `executeSideBySideJob()` runs:
- Both algorithms sort the same shuffled array
- Shorter state list is padded to match the longer one
- `SideBySideRenderer` renders each half independently, composites with divider
- Each half gets its own glow/particle effects and title overlay
- Stats overlay spans the full frame
- Audio uses the left algorithm's states

## Adding a New Sorting Algorithm

1. Create `src/main/java/io/github/pgatzka/videogen/algorithm/MyNewSortAlgorithm.java`
2. Implement `SortingAlgorithm` interface
3. Annotate with `@Component` and `@Slf4j`
4. The algorithm receives `int[] input` and must return `List<SortingState>`
5. Each comparison should emit a state with `compareIdx1`/`compareIdx2` set
6. Each swap should emit a state with `swapped=true`
7. Final state should have all indices in the `sorted` set
8. It will be auto-discovered by `AlgorithmRegistry`

```java
@Slf4j
@Component
public class MyNewSortAlgorithm implements SortingAlgorithm {
  @Override
  public String getName() { return "MyNewSort"; }

  @Override
  public List<SortingState> sort(int[] input) {
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();
    states.add(new SortingState(array, -1, -1, false, sorted));
    // ... sorting logic, emit states for each comparison/swap ...
    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));
    return states;
  }
}
```

**Important**: For goofy/slow algorithms, consider capping iterations (see `BogoSortAlgorithm` MAX_ATTEMPTS). The `ScheduledVideoPublisher` filters out BogoSort, StoogeSort, and SlowSort from random generation.

## Adding a New Visualization

1. Create `src/main/java/io/github/pgatzka/videogen/visualization/MyNewVisualization.java`
2. Implement `Visualization` interface
3. Annotate with `@Component` and `@Slf4j`
4. Must return `BufferedImage.TYPE_3BYTE_BGR` (required by FFmpeg encoder)
5. Use `ColorScheme` methods for colors:
   - `colorScheme.getBarColor(value, maxValue)` — default color based on element value
   - `colorScheme.getSortedColor(value, maxValue)` — color for sorted elements
   - `colorScheme.getCompareColor()` — color for compared elements
   - `colorScheme.getSwappedColor()` — color for swapped elements
6. Color priority: sorted → swapped+compare → compare → default
7. Auto-discovered by `VisualizationRegistry`

```java
@Slf4j
@Component
public class MyNewVisualization implements Visualization {
  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);

  @Override
  public String getName() { return "MyNew"; }

  @Override
  public BufferedImage renderFrame(SortingState state, int width, int height, ColorScheme colorScheme) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(BACKGROUND);
    g.fillRect(0, 0, width, height);
    // ... render elements ...
    g.dispose();
    return image;
  }
}
```

## Adding a New Visual Effect

Effects are applied in `FramePostProcessor.process()`. To add a new one:

1. Create the effect class in `visualization/`
2. It receives a `BufferedImage` (TYPE_3BYTE_BGR) and must draw on it
3. **For transparency**: create a `TYPE_INT_ARGB` overlay, draw on it, then composite onto the BGR image:
   ```java
   BufferedImage overlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
   Graphics2D og = overlay.createGraphics();
   // draw with alpha...
   og.dispose();
   Graphics2D g = bgrImage.createGraphics();
   g.drawImage(overlay, 0, 0, null);
   g.dispose();
   ```
4. Add a toggle field to `VideoJobEntity`, `VideoJobRequest`, migration, properties, UI
5. Wire it in `FramePostProcessor`

**Key gotcha**: `TYPE_3BYTE_BGR` does NOT support alpha. Semi-transparent drawing directly on it is silently ignored. Always use an ARGB overlay.

## Adding a New Toggleable Feature

Pattern for any new boolean feature:

1. **Entity**: Add field to `VideoJobEntity` with `@Column(nullable = false)` and default value
2. **Migration**: Create `V{N}__add_{name}_column.sql`: `ALTER TABLE video_job ADD COLUMN {name} BOOLEAN NOT NULL DEFAULT FALSE;`
3. **Request DTO**: Add field to `VideoJobRequest`
4. **Properties**: Add `default{Name}` to `ApplicationProperties`
5. **Service**: Add `entity.set{Name}(request.is{Name}())` in `createJob()`
6. **UI**: Add `Checkbox` in `VideoGeneratorView`, wire to request builder
7. **i18n**: Add label in `translations.properties`
8. **Logic**: Implement in `VideoJobService.executeJob()` or relevant class

## VideoJobEntity Fields

### Job Configuration
`algorithm`, `visualization`, `elementCount`, `fps`, `width`, `height`, `framesPerStep`, `colorScheme`, `shuffle`, `sound`, `showStats`, `glowEffect`, `particleTrail`, `tweening`, `speedRun`, `debug`, `autoUpload`, `secondAlgorithm`

### Job Status
`status` (QUEUED/RUNNING/UPLOADING/COMPLETED/FAILED), `progress` (0-100), `statusMessage`, `errorMessage`, `outputPath`, `createdAt`, `startedAt`, `completedAt`

### YouTube
`youtubeVideoId`, `youtubeStatus`, `youtubeViews`, `youtubeLikes`, `youtubeComments`, `youtubeTitle`, `metricsUpdatedAt`

## Available Algorithms (17)

**Standard**: BubbleSort, QuickSort, InsertionSort, SelectionSort, MergeSort, HeapSort, ShellSort, CocktailShakerSort, CombSort, GnomeSort, BitonicSort

**Goofy**: BogoSort (capped 10K attempts), StalinSort, PancakeSort, StoogeSort, SlowSort, GravitySort

## Available Visualizations (10)

BarChart, Circle, DotPlot, Spiral, ColorGradient, Disparity, Ring, Pyramid, ScatterPlot, PixelGrid

## Available Color Schemes (3)

DEFAULT (cyan/orange/red/green), RAINBOW (HSB by value), GREYSCALE

## Audio System

`AudioGenerator` takes a list of `AudioPhase(paddingFrames, states)`. For each state:
- If `compareIdx1 >= 0 && compareIdx2 >= 0`: tone at average frequency of both values
- If only `compareIdx1 >= 0`: tone at that value's frequency (used for victory sweep)
- Otherwise: silence

Frequencies: 200Hz (value=1) to 1400Hz (value=max). Sine waves with fade-in/fade-out envelope. Generated as 16-bit mono WAV at 44100Hz. Muxed with video via FFmpeg (`-c:a aac -b:a 192k`).

## Stats Overlay

- **Duration**: Only counts during sort phase (not shuffle, not padding). Uses `startCounting()`/`freeze()`.
- **Comparisons/Array accesses**: Only counted during sort phase. Shuffle passes `countStats=false` to `writeStates()`.
- **Debug mode**: Shows all job config in muted grey text below the fun fact.
- **Font sizes**: Scaled for TikTok 1080x1920 portrait (`width/18` for labels).

## YouTube Integration

- **OAuth2 Device Flow**: No redirect URI needed. User visits google.com/device and enters a code. Tokens persisted to `youtube-tokens.properties` (or `/app/tokens/` in Docker).
- **Upload**: Resumable upload via YouTube Data API v3. Category 28 (Science & Technology).
- **Auto-upload**: Jobs with `autoUpload=true` upload after encoding and wait for the video to be publicly available before marking COMPLETED. Upload failures don't fail the job.
- **Scheduled**: Every 4 hours, generates a random video with `autoUpload=true`.
- **Metrics**: Refreshed hourly. Views, likes, comments stored on entity.
- **Quota**: Default 10,000 units/day (~6 uploads). Request increase via Google Cloud console.

## Environment Variables

```bash
# Required for production
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/sorting
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# YouTube (optional)
YOUTUBE_ENABLED=true
YOUTUBE_CLIENT_ID=xxx
YOUTUBE_CLIENT_SECRET=xxx
# YOUTUBE_REFRESH_TOKEN is auto-managed via device auth flow
```

## Docker

Two compose files:

- **`docker-compose-dev.yml`** — local development with PostgreSQL included. Reads secrets from `.env` file (gitignored).
  ```bash
  docker compose -f docker-compose-dev.yml up -d --build
  ```

- **`docker-compose.yml`** — production/Portainer. App only, no database (expects external PostgreSQL). Pulls pre-built image from GHCR.
  ```bash
  docker compose up -d
  ```
  Configure via Portainer environment variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `YOUTUBE_ENABLED`, `YOUTUBE_CLIENT_ID`, `YOUTUBE_CLIENT_SECRET`.

Volumes: `videos` (generated files), `tokens` (YouTube OAuth tokens). App runs on port **8090**. UI at `/`, analytics at `/analytics`.

## Branches

- **`main`** — development branch. Always has a `-SNAPSHOT` version. CI runs on every push/PR.
- **`production`** — release-only branch. Updated automatically by the release workflow. Always points to the latest release tag. Used by Portainer to pull the production compose file.

## Profiles

- **quiet** - Disables Spring banner, sets `org.atmosphere.cpr` to warn. Active via Maven by default.
- **dev** - PostgreSQL credentials for local development, `ddl-auto: update`. Active via Maven by default.
- **production** - Vaadin production build (frontend bundled). Used in Docker and releases.

## Code Style

- **Google Java Format** enforced via Spotless. Run `./mvnw spotless:apply` before committing.
- Always run spotless before committing to avoid CI failures.

## CI/CD (GitHub Actions)

### CI Pipeline (`ci.yml`)
Spotless Check → Tests (with FFmpeg) → SonarCloud → Docker build & push to GHCR.

### Release Pipeline (`release.yml`)
Manual trigger with version bump dropdown (patch/minor/major):
1. Calculates release + next-dev versions from current pom
2. `mvn release:prepare` — commits release version, tags, commits next SNAPSHOT, pushes
3. Docker build & push tagged with release version + `latest`
4. Creates GitHub Release with auto-generated notes

## Key Design Decisions

- **`VideoJobRequest` builder DTO** instead of long parameter lists (was 11+ params before refactor)
- **`FramePostProcessor`** bundles all per-frame effects to keep `writeStates()` clean
- **`REQUIRES_NEW` transactions** on `VideoJobUpdater` so progress updates commit independently of the main job transaction
- **Separate `writeStates()` and `writePadding()`** methods with consistent signatures for all phases
- **Upload as part of job pipeline** (not external) — job status shows UPLOADING and waits for YouTube to process
- **Glow/particle effects use ARGB overlay compositing** because TYPE_3BYTE_BGR doesn't support alpha
- **Color based on value, not position** — so RAINBOW colors follow elements as they move during sorting
- **Stats only count sort phase** — shuffle/sweep don't affect duration, comparisons, or array accesses
