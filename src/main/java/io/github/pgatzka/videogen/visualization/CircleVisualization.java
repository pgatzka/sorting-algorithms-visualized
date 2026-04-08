package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CircleVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final float MIN_RADIUS_RATIO = 0.15f;
  private static final float MAX_RADIUS_RATIO = 0.45f;

  @Override
  public String getName() {
    return "Circle";
  }

  @Override
  public BufferedImage renderFrame(
      SortingState state, int width, int height, ColorScheme colorScheme) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = image.createGraphics();

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

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
    int maxDimension = Math.min(width, height);
    float minRadius = maxDimension * MIN_RADIUS_RATIO;
    float maxRadius = maxDimension * MAX_RADIUS_RATIO;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) {
        maxVal = val;
      }
    }

    double angleStep = 2.0 * Math.PI / n;
    float lineWidth = Math.max(1.5f, (float) (2.0 * Math.PI * maxRadius / n) * 0.6f);
    g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    for (int i = 0; i < n; i++) {
      double angle = i * angleStep - Math.PI / 2; // start from top
      float ratio = (float) array[i] / maxVal;
      float radius = minRadius + ratio * (maxRadius - minRadius);

      int x1 = (int) (centerX + minRadius * Math.cos(angle));
      int y1 = (int) (centerY + minRadius * Math.sin(angle));
      int x2 = (int) (centerX + radius * Math.cos(angle));
      int y2 = (int) (centerY + radius * Math.sin(angle));

      g.setColor(getLineColor(state, i, maxVal, colorScheme));
      g.drawLine(x1, y1, x2, y2);
    }

    g.dispose();
    return image;
  }

  private Color getLineColor(SortingState state, int index, int maxValue, ColorScheme colorScheme) {
    int value = state.array()[index];
    if (state.sorted().contains(index)) {
      return colorScheme.getSortedColor(value, maxValue);
    }
    if (state.swapped() && (index == state.compareIdx1() || index == state.compareIdx2())) {
      return colorScheme.getSwappedColor();
    }
    if (index == state.compareIdx1() || index == state.compareIdx2()) {
      return colorScheme.getCompareColor();
    }
    return colorScheme.getBarColor(value, maxValue);
  }
}
