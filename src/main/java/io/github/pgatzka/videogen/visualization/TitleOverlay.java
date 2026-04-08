package io.github.pgatzka.videogen.visualization;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TitleOverlay {

  private final String algorithmName;

  public TitleOverlay(String algorithmName) {
    this.algorithmName = algorithmName;
  }

  public void render(BufferedImage image) {
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    int width = image.getWidth();
    int height = image.getHeight();
    int fontSize = Math.max(28, width / 14);
    Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
    g.setFont(font);
    FontMetrics fm = g.getFontMetrics();

    String text = algorithmName;
    int textWidth = fm.stringWidth(text);
    int x = (width - textWidth) / 2;
    int y = height - Math.max(30, height / 20);

    // Shadow
    g.setColor(new Color(0, 0, 0, 180));
    g.drawString(text, x + 2, y + 2);

    // Text
    g.setColor(Color.WHITE);
    g.drawString(text, x, y);

    g.dispose();
  }
}
