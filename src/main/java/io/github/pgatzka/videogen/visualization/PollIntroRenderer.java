package io.github.pgatzka.videogen.visualization;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PollIntroRenderer {

  private static final Color BACKGROUND = new Color(0x1a, 0x1a, 0x2e);
  private static final Color ACCENT = new Color(0x00, 0xD2, 0xD3);
  private static final Color VS_COLOR = new Color(0xFF, 0x6B, 0x35);

  public BufferedImage renderSingleAlgorithm(
      String algorithmName, int elementCount, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = setupGraphics(image);

    int titleSize = Math.max(36, width / 10);
    int subtitleSize = Math.max(24, width / 16);

    // Algorithm name
    Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, titleSize);
    g.setFont(titleFont);
    g.setColor(ACCENT);
    drawCentered(g, algorithmName, width, height / 2 - titleSize);

    // Element count
    Font subFont = new Font(Font.MONOSPACED, Font.PLAIN, subtitleSize);
    g.setFont(subFont);
    g.setColor(Color.WHITE);
    drawCentered(g, elementCount + " elements", width, height / 2 + subtitleSize);

    g.dispose();
    return image;
  }

  public BufferedImage renderVersus(
      String algorithm1, String algorithm2, int elementCount, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = setupGraphics(image);

    int nameSize = Math.max(32, width / 12);
    int vsSize = Math.max(48, width / 8);
    int subtitleSize = Math.max(24, width / 16);
    int centerY = height / 2;

    // "Who wins?"
    Font questionFont = new Font(Font.MONOSPACED, Font.ITALIC, subtitleSize);
    g.setFont(questionFont);
    g.setColor(new Color(0xFF, 0xD9, 0x3D));
    drawCentered(g, "Who wins?", width, centerY - nameSize * 3);

    // Algorithm 1
    Font nameFont = new Font(Font.MONOSPACED, Font.BOLD, nameSize);
    g.setFont(nameFont);
    g.setColor(ACCENT);
    drawCentered(g, algorithm1, width, centerY - nameSize);

    // VS
    Font vsFont = new Font(Font.MONOSPACED, Font.BOLD, vsSize);
    g.setFont(vsFont);
    g.setColor(VS_COLOR);
    drawCentered(g, "VS", width, centerY + vsSize / 3);

    // Algorithm 2
    g.setFont(nameFont);
    g.setColor(ACCENT);
    drawCentered(g, algorithm2, width, centerY + nameSize + vsSize / 2);

    // Element count
    Font subFont = new Font(Font.MONOSPACED, Font.PLAIN, subtitleSize);
    g.setFont(subFont);
    g.setColor(Color.WHITE);
    drawCentered(g, elementCount + " elements", width, centerY + nameSize * 2 + vsSize);

    g.dispose();
    return image;
  }

  private Graphics2D setupGraphics(BufferedImage image) {
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setColor(BACKGROUND);
    g.fillRect(0, 0, image.getWidth(), image.getHeight());
    return g;
  }

  private void drawCentered(Graphics2D g, String text, int width, int y) {
    FontMetrics fm = g.getFontMetrics();
    int x = (width - fm.stringWidth(text)) / 2;
    g.drawString(text, x, y);
  }
}
