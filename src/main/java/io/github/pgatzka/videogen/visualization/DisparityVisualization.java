package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DisparityVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final int PADDING = 80;

  @Override
  public String getName() {
    return "Disparity";
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
    int topY = PADDING;
    int bottomY = PADDING + drawH;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    float lineWidth = Math.max(1.0f, (float) drawW / n * 0.6f);
    g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    for (int i = 0; i < n; i++) {
      // Current position (top row)
      float currentX = PADDING + ((float) i / (n - 1)) * drawW;
      // Target position (bottom row) — value determines where it should be
      float targetX = PADDING + ((float) (array[i] - 1) / (maxVal - 1)) * drawW;

      g.setColor(getLineColor(state, i, maxVal, colorScheme));
      g.drawLine(Math.round(currentX), topY, Math.round(targetX), bottomY);
    }

    g.dispose();
    return image;
  }

  private Color getLineColor(SortingState state, int index, int maxVal, ColorScheme colorScheme) {
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
