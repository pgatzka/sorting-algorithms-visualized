package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RingVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final float INNER_RATIO = 0.15f;
  private static final float MAX_OUTER_RATIO = 0.45f;

  @Override
  public String getName() {
    return "Ring";
  }

  @Override
  public BufferedImage renderFrame(
      SortingState state, int width, int height, ColorScheme colorScheme) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g.setColor(BACKGROUND);
    g.fillRect(0, 0, width, height);

    int[] array = state.array();
    int n = array.length;
    if (n == 0) {
      g.dispose();
      return image;
    }

    int centerX = width / 2;
    int centerY = height / 2;
    int maxDim = Math.min(width, height);
    float innerRadius = maxDim * INNER_RATIO;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    double arcExtent = 360.0 / n;

    for (int i = 0; i < n; i++) {
      double startAngle = 90 - i * arcExtent; // start from top, clockwise

      float valueRatio = (float) array[i] / maxVal;
      float outerRadius = innerRadius + valueRatio * (maxDim * MAX_OUTER_RATIO - innerRadius);

      // Draw filled wedge: outer arc minus inner circle
      g.setColor(getWedgeColor(state, i, maxVal, colorScheme));

      // Outer arc
      Arc2D outer =
          new Arc2D.Float(
              centerX - outerRadius,
              centerY - outerRadius,
              outerRadius * 2,
              outerRadius * 2,
              (float) startAngle,
              (float) -arcExtent,
              Arc2D.PIE);
      g.fill(outer);

      // Cut out inner circle (overdraw with background)
      // We'll draw all wedges first then cut the inner circle
    }

    // Cut out the inner circle
    g.setColor(BACKGROUND);
    g.fillOval(
        Math.round(centerX - innerRadius),
        Math.round(centerY - innerRadius),
        Math.round(innerRadius * 2),
        Math.round(innerRadius * 2));

    g.dispose();
    return image;
  }

  private Color getWedgeColor(SortingState state, int index, int maxVal, ColorScheme colorScheme) {
    int value = state.array()[index];
    if (state.sorted().contains(index)) {
      return colorScheme.getSortedColor(value, maxVal);
    }
    if (state.swapped() && (index == state.compareIdx1() || index == state.compareIdx2())) {
      return colorScheme.getSwappedColor();
    }
    if (index == state.compareIdx1() || index == state.compareIdx2()) {
      return colorScheme.getCompareColor();
    }
    return colorScheme.getBarColor(value, maxVal);
  }
}
