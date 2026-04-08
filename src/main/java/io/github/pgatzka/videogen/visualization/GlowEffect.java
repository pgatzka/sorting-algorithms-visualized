package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GlowEffect {

  private static final Color GLOW_COMPARE = new Color(0xFF, 0x6B, 0x35);
  private static final Color GLOW_SWAP = new Color(0xFF, 0x35, 0x35);
  private static final int GLOW_RADIUS = 15;
  private static final int SAMPLE_STEP = 8;

  public void apply(BufferedImage image, SortingState state, int elementCount) {
    if (state.compareIdx1() < 0 && state.compareIdx2() < 0) {
      return;
    }

    int width = image.getWidth();
    int height = image.getHeight();
    Color glowColor = state.swapped() ? GLOW_SWAP : GLOW_COMPARE;
    Color compareColor = new Color(0xFF, 0x6B, 0x35);
    Color swapColor = new Color(0xFF, 0x35, 0x35);

    // Scan the rendered frame for highlighted pixels (compare/swap colors)
    // and paint a glow around them onto an ARGB overlay
    BufferedImage overlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D og = overlay.createGraphics();
    og.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int glowRadius = Math.max(GLOW_RADIUS, Math.min(width, height) / 60);

    for (int y = 0; y < height; y += SAMPLE_STEP) {
      for (int x = 0; x < width; x += SAMPLE_STEP) {
        int rgb = image.getRGB(x, y);
        if (isHighlightColor(rgb, compareColor) || isHighlightColor(rgb, swapColor)) {
          for (int layer = 2; layer >= 0; layer--) {
            int r = glowRadius + layer * glowRadius;
            int alpha = 6 - layer * 2;
            og.setColor(
                new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha));
            og.fillOval(x - r, y - r, r * 2, r * 2);
          }
        }
      }
    }
    og.dispose();

    // Composite the ARGB overlay onto the BGR image
    Graphics2D g = image.createGraphics();
    g.drawImage(overlay, 0, 0, null);
    g.dispose();
  }

  private boolean isHighlightColor(int rgb, Color target) {
    int r = (rgb >> 16) & 0xFF;
    int g2 = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    return Math.abs(r - target.getRed()) < 20
        && Math.abs(g2 - target.getGreen()) < 20
        && Math.abs(b - target.getBlue()) < 20;
  }
}
