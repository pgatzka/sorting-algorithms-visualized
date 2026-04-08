package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PixelGridVisualization implements Visualization {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final int PADDING = 40;

  @Override
  public String getName() {
    return "PixelGrid";
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

    // Calculate grid dimensions — try to make it roughly square
    int cols = (int) Math.ceil(Math.sqrt((double) n * drawW / drawH));
    int rows = (int) Math.ceil((double) n / cols);

    float cellW = (float) drawW / cols;
    float cellH = (float) drawH / rows;

    int maxVal = 1;
    for (int val : array) {
      if (val > maxVal) maxVal = val;
    }

    for (int i = 0; i < n; i++) {
      int col = i % cols;
      int row = i / cols;

      int x = PADDING + Math.round(col * cellW);
      int y = PADDING + Math.round(row * cellH);
      int w = Math.max(1, Math.round(cellW));
      int h = Math.max(1, Math.round(cellH));

      g.setColor(getCellColor(state, i, maxVal, colorScheme));
      g.fillRect(x, y, w, h);

      // Highlight border for compared/swapped
      if (state.swapped() && (i == state.compareIdx1() || i == state.compareIdx2())) {
        g.setColor(colorScheme.getSwappedColor());
        g.setStroke(new BasicStroke(3));
        g.drawRect(x, y, w, h);
      } else if (i == state.compareIdx1() || i == state.compareIdx2()) {
        g.setColor(colorScheme.getCompareColor());
        g.setStroke(new BasicStroke(3));
        g.drawRect(x, y, w, h);
      }
    }

    g.dispose();
    return image;
  }

  private Color getCellColor(SortingState state, int index, int maxVal, ColorScheme colorScheme) {
    int value = state.array()[index];
    // Always use HSB coloring for the grid — the gradient is the whole point
    float hue = (float) value / maxVal;
    if (state.sorted().contains(index)) {
      return Color.getHSBColor(hue, 0.9f, 1.0f);
    }
    return Color.getHSBColor(hue, 0.7f, 0.8f);
  }
}
