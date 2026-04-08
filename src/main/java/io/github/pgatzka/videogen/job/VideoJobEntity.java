package io.github.pgatzka.videogen.job;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "video_job")
public class VideoJobEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 50)
  private String algorithm;

  @Column(nullable = false, length = 50)
  private String visualization;

  @Column(name = "element_count", nullable = false)
  private int elementCount;

  @Column(nullable = false)
  private int fps;

  @Column(nullable = false)
  private int width;

  @Column(nullable = false)
  private int height;

  @Column(name = "frames_per_step", nullable = false)
  private int framesPerStep;

  @Column(nullable = false)
  private boolean shuffle;

  @Column(name = "color_scheme", nullable = false, length = 20)
  private String colorScheme = "DEFAULT";

  @Column(nullable = false)
  private boolean sound;

  @Column(name = "show_stats", nullable = false)
  private boolean showStats;

  @Column(name = "glow_effect", nullable = false)
  private boolean glowEffect;

  @Column(name = "particle_trail", nullable = false)
  private boolean particleTrail;

  @Column(nullable = false)
  private boolean tweening;

  @Column(name = "speed_run", nullable = false)
  private boolean speedRun;

  @Column(name = "second_algorithm", length = 50)
  private String secondAlgorithm;

  @Column(nullable = false)
  private boolean debug;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VideoJobStatus status = VideoJobStatus.QUEUED;

  @Column(nullable = false)
  private int progress;

  @Column(name = "output_path", length = 500)
  private String outputPath;

  @Column(name = "status_message", length = 500)
  private String statusMessage;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
