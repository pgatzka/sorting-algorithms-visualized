package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.image.BufferedImage;

public class FramePostProcessor {

  private final TitleOverlay titleOverlay;
  private final StatsOverlay statsOverlay;
  private final GlowEffect glowEffect;
  private final ParticleTrailEffect particleTrail;
  private final int elementCount;

  public FramePostProcessor(
      String algorithmName,
      StatsOverlay statsOverlay,
      boolean glowEnabled,
      boolean particleEnabled,
      int elementCount) {
    this.titleOverlay = new TitleOverlay(algorithmName);
    this.statsOverlay = statsOverlay;
    this.glowEffect = glowEnabled ? new GlowEffect() : null;
    this.particleTrail = particleEnabled ? new ParticleTrailEffect() : null;
    this.elementCount = elementCount;
  }

  public void process(
      BufferedImage frame, SortingState state, int framesWritten, int fps, int[] counters) {
    // Glow scans the rendered frame for highlighted colors and adds bloom
    if (glowEffect != null) {
      glowEffect.apply(frame, state, elementCount);
    }
    // Particles spawn at swap-colored pixels found in the rendered frame
    if (particleTrail != null) {
      particleTrail.update(state, frame);
      particleTrail.render(frame);
    }
    if (statsOverlay != null) {
      statsOverlay.render(frame, framesWritten, fps, counters[0], counters[1]);
    }
    titleOverlay.render(frame);
  }

  public StatsOverlay getStatsOverlay() {
    return statsOverlay;
  }
}
