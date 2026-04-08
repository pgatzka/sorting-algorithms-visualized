package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BarChartVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final Color BAR_DEFAULT = new Color(0x00, 0xd2, 0xd3);
  private static final Color BAR_COMPARE = new Color(0xff, 0x6b, 0x35);
  private static final Color BAR_SWAPPED = new Color(0xff, 0x35, 0x35);
  private static final Color BAR_SORTED = new Color(0x2e, 0xcc, 0x71);

  private static final int PADDING_TOP = 80;
  private static final int PADDING_BOTTOM = 80;
  private static final int PADDING_SIDE = 40;
  private static final float GAP_RATIO = 0.2f;

  @Override
  public String getName() {
    return "BarChart";
  }

  @Override
  public BufferedImage renderFrame(SortingState state, int width, int height) {
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

    int drawableWidth = width - 2 * PADDING_SIDE;
    int drawableHeight = height - PADDING_TOP - PADDING_BOTTOM;

    float totalBarWidth = (float) drawableWidth / n;
    float gap = totalBarWidth * GAP_RATIO;
    float barWidth = totalBarWidth - gap;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) {
        maxVal = val;
      }
    }

    for (int i = 0; i < n; i++) {
      float barHeight = ((float) array[i] / maxVal) * drawableHeight;
      float x = PADDING_SIDE + i * totalBarWidth + gap / 2;
      float y = height - PADDING_BOTTOM - barHeight;

      g.setColor(getBarColor(state, i));
      g.fillRoundRect(
          Math.round(x),
          Math.round(y),
          Math.max(1, Math.round(barWidth)),
          Math.round(barHeight),
          4,
          4);
    }

    g.dispose();
    return image;
  }

  private Color getBarColor(SortingState state, int index) {
    if (state.sorted().contains(index)) {
      return BAR_SORTED;
    }
    if (state.swapped() && (index == state.compareIdx1() || index == state.compareIdx2())) {
      return BAR_SWAPPED;
    }
    if (index == state.compareIdx1() || index == state.compareIdx2()) {
      return BAR_COMPARE;
    }
    return BAR_DEFAULT;
  }
}
