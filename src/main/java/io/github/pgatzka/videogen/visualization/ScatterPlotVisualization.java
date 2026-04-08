package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScatterPlotVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final Color LINE_COLOR = new Color(0x40, 0x40, 0x60);
  private static final int PADDING = 80;

  @Override
  public String getName() {
    return "ScatterPlot";
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

    int drawW = width - 2 * PADDING;
    int drawH = height - 2 * PADDING;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    // Calculate point positions
    int[] px = new int[n];
    int[] py = new int[n];
    for (int i = 0; i < n; i++) {
      px[i] = PADDING + Math.round(((float) i / (n - 1)) * drawW);
      py[i] = PADDING + drawH - Math.round(((float) array[i] / maxVal) * drawH);
    }

    // Draw connecting lines
    float lineWidth = Math.max(1.0f, (float) Math.min(width, height) / 500);
    g.setStroke(new BasicStroke(lineWidth));
    g.setColor(LINE_COLOR);
    for (int i = 0; i < n - 1; i++) {
      g.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
    }

    // Draw dots on top
    int dotSize = Math.max(4, Math.min(drawW, drawH) / n);
    for (int i = 0; i < n; i++) {
      g.setColor(getDotColor(state, i, maxVal, colorScheme));
      g.fillOval(px[i] - dotSize / 2, py[i] - dotSize / 2, dotSize, dotSize);
    }

    g.dispose();
    return image;
  }

  private Color getDotColor(SortingState state, int index, int maxVal, ColorScheme colorScheme) {
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
