package io.github.pgatzka.videogen.job;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoJobRequest {

  private String algorithm;
  private String visualization;
  private int elementCount;
  private int fps;
  private int width;
  private int height;
  private int framesPerStep;
  private boolean shuffle;
  private String colorScheme;
  private boolean sound;
  private boolean showStats;
  private boolean glowEffect;
  private boolean particleTrail;
  private boolean tweening;
  private boolean speedRun;

  // Side-by-side comparison
  private String secondAlgorithm;

  private boolean debug;
  private boolean autoUpload;
}
