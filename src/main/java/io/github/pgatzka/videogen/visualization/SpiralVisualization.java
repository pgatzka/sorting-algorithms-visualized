package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpiralVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final double SPIRAL_TURNS = 3.0;

  @Override
  public String getName() {
    return "Spiral";
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
    float maxRadius = maxDim * 0.45f;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    float lineWidth = Math.max(1.5f, (float) maxDim / n * 0.8f);
    g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    for (int i = 0; i < n; i++) {
      double t = (double) i / n;
      double angle = t * SPIRAL_TURNS * 2.0 * Math.PI - Math.PI / 2;
      float baseRadius = (float) (t * maxRadius * 0.8);

      float valueRatio = (float) array[i] / maxVal;
      float extensionLength = maxRadius * 0.15f * valueRatio;

      int x1 = (int) (centerX + baseRadius * Math.cos(angle));
      int y1 = (int) (centerY + baseRadius * Math.sin(angle));
      int x2 = (int) (centerX + (baseRadius + extensionLength) * Math.cos(angle));
      int y2 = (int) (centerY + (baseRadius + extensionLength) * Math.sin(angle));

      g.setColor(getColor(state, i, maxVal, colorScheme));
      g.drawLine(x1, y1, x2, y2);
    }

    g.dispose();
    return image;
  }

  private Color getColor(SortingState state, int index, int maxVal, ColorScheme colorScheme) {
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
