package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PyramidVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final int PADDING_TOP = 80;
  private static final int PADDING_BOTTOM = 80;
  private static final int PADDING_SIDE = 40;
  private static final float GAP_RATIO = 0.15f;

  @Override
  public String getName() {
    return "Pyramid";
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

    int drawH = height - PADDING_TOP - PADDING_BOTTOM;
    int halfW = (width - 2 * PADDING_SIDE) / 2;
    int centerX = width / 2;

    float totalBarHeight = (float) drawH / n;
    float gap = totalBarHeight * GAP_RATIO;
    float barHeight = totalBarHeight - gap;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    for (int i = 0; i < n; i++) {
      float barWidth = ((float) array[i] / maxVal) * halfW;
      float y = PADDING_TOP + i * totalBarHeight + gap / 2;

      g.setColor(getBarColor(state, i, maxVal, colorScheme));
      g.fillRoundRect(
          Math.round(centerX - barWidth),
          Math.round(y),
          Math.round(barWidth * 2),
          Math.max(1, Math.round(barHeight)),
          4,
          4);
    }

    g.dispose();
    return image;
  }

  private Color getBarColor(SortingState state, int index, int maxVal, ColorScheme colorScheme) {
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
