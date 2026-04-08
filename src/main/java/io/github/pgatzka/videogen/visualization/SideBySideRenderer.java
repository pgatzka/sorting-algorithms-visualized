package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SideBySideRenderer {

  private static final Color DIVIDER_COLOR = new Color(0x40, 0x40, 0x60);

  public BufferedImage render(
      Visualization visualization,
      SortingState leftState,
      SortingState rightState,
      int width,
      int height,
      ColorScheme colorScheme) {

    int halfWidth = width / 2;

    BufferedImage leftFrame = visualization.renderFrame(leftState, halfWidth, height, colorScheme);
    BufferedImage rightFrame =
        visualization.renderFrame(rightState, halfWidth, height, colorScheme);

    BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = combined.createGraphics();

    g.drawImage(leftFrame, 0, 0, null);
    g.drawImage(rightFrame, halfWidth, 0, null);

    // Divider line
    g.setColor(DIVIDER_COLOR);
    g.setStroke(new BasicStroke(2));
    g.drawLine(halfWidth, 0, halfWidth, height);

    g.dispose();
    return combined;
  }
}
