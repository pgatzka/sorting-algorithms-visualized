package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParticleTrailEffect {

  private static final int MAX_PARTICLES = 600;
  private static final int PARTICLE_LIFETIME = 20;
  private static final Color SWAP_COLOR_TARGET = new Color(0xFF, 0x35, 0x35);
  private static final int SAMPLE_STEP = 6;

  private final List<Particle> particles = new ArrayList<>();

  public void update(SortingState state, BufferedImage renderedFrame) {
    // Spawn particles at actual swap-colored pixel locations from the rendered frame
    if (state.swapped() && state.compareIdx1() >= 0) {
      spawnFromFrame(renderedFrame);
    }

    // Age and remove dead particles
    Iterator<Particle> it = particles.iterator();
    while (it.hasNext()) {
      Particle p = it.next();
      p.age++;
      p.x += p.vx;
      p.y += p.vy;
      p.vy += 0.5f; // gravity
      if (p.age > PARTICLE_LIFETIME) {
        it.remove();
      }
    }
  }

  public void render(BufferedImage image) {
    if (particles.isEmpty()) {
      return;
    }

    // Draw particles onto an ARGB overlay, then composite onto the BGR image
    BufferedImage overlay =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D og = overlay.createGraphics();
    og.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (Particle p : particles) {
      float life = 1.0f - (float) p.age / PARTICLE_LIFETIME;
      int alpha = (int) (life * 255);
      int size = Math.max(4, (int) (life * 16));
      og.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
      og.fillOval(Math.round(p.x - size / 2f), Math.round(p.y - size / 2f), size, size);
      // Bright core
      int coreSize = Math.max(2, size / 2);
      og.setColor(new Color(255, 255, 255, alpha / 2));
      og.fillOval(
          Math.round(p.x - coreSize / 2f), Math.round(p.y - coreSize / 2f), coreSize, coreSize);
    }
    og.dispose();

    Graphics2D g = image.createGraphics();
    g.drawImage(overlay, 0, 0, null);
    g.dispose();
  }

  private void spawnFromFrame(BufferedImage frame) {
    int width = frame.getWidth();
    int height = frame.getHeight();
    // Find swap-colored pixels and spawn particles there
    List<int[]> swapPixels = new ArrayList<>();
    for (int y = 0; y < height; y += SAMPLE_STEP) {
      for (int x = 0; x < width; x += SAMPLE_STEP) {
        int rgb = frame.getRGB(x, y);
        if (isSwapColor(rgb)) {
          swapPixels.add(new int[] {x, y});
        }
      }
    }

    if (swapPixels.isEmpty()) {
      return;
    }

    // Spawn particles at random swap pixel locations
    int count = Math.min(15, swapPixels.size());
    for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
      int[] pos = swapPixels.get((int) (Math.random() * swapPixels.size()));
      float angle = (float) (Math.random() * 2 * Math.PI);
      float speed = (float) (Math.random() * 7 + 3);
      particles.add(
          new Particle(
              pos[0],
              pos[1],
              (float) (speed * Math.cos(angle)),
              (float) (speed * Math.sin(angle)),
              new Color(
                  0xFF, 0x35 + (int) (Math.random() * 0x60), 0x20 + (int) (Math.random() * 0x30))));
    }
  }

  private boolean isSwapColor(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    return Math.abs(r - SWAP_COLOR_TARGET.getRed()) < 25
        && Math.abs(g - SWAP_COLOR_TARGET.getGreen()) < 25
        && Math.abs(b - SWAP_COLOR_TARGET.getBlue()) < 25;
  }

  private static class Particle {
    float x, y, vx, vy;
    int age;
    Color color;

    Particle(float x, float y, float vx, float vy, Color color) {
      this.x = x;
      this.y = y;
      this.vx = vx;
      this.vy = vy;
      this.color = color;
    }
  }
}
