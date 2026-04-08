package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ColorGradientVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final int PADDING = 40;

  @Override
  public String getName() {
    return "ColorGradient";
  }

  @Override
  public BufferedImage renderFrame(
      SortingState state, int width, int height, ColorScheme colorScheme) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = image.createGraphics();

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
    float stripHeight = (float) drawH / n;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    for (int i = 0; i < n; i++) {
      int y = PADDING + Math.round(i * stripHeight);
      int h = Math.max(1, Math.round(stripHeight));

      float hue = (float) array[i] / maxVal;
      g.setColor(Color.getHSBColor(hue, 0.8f, 0.9f));
      g.fillRect(PADDING, y, drawW, h);

      // Highlight compared/swapped with left accent bar
      if (state.swapped() && (i == state.compareIdx1() || i == state.compareIdx2())) {
        g.setColor(colorScheme.getSwappedColor());
        g.fillRect(PADDING, y, 12, h);
        g.fillRect(PADDING + drawW - 12, y, 12, h);
      } else if (i == state.compareIdx1() || i == state.compareIdx2()) {
        g.setColor(colorScheme.getCompareColor());
        g.fillRect(PADDING, y, 12, h);
        g.fillRect(PADDING + drawW - 12, y, 12, h);
      }
    }

    g.dispose();
    return image;
  }
}
