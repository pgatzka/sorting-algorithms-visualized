package io.github.pgatzka.videogen.visualization;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class StatsOverlay {

  private static final List<String> FUN_FACTS =
      List.of(
          "Bubble Sort was analyzed as early as 1956",
          "Quicksort was invented by Tony Hoare in 1959",
          "Bogosort has an average time complexity of O(n \u00d7 n!)",
          "Timsort, used in Python and Java, was invented in 2002",
          "The lower bound for comparison sorts is O(n log n)",
          "Radix Sort can beat O(n log n) by not comparing elements",
          "Merge Sort guarantees O(n log n) even in the worst case",
          "Sleep Sort creates a thread per element that sleeps for its value",
          "Spaghetti Sort is an O(n) analog sorting algorithm using pasta",
          "Stalin Sort removes unsorted elements \u2014 always O(n)");

  private static final Color BG_COLOR = new Color(0, 0, 0, 160);
  private static final Color TEXT_COLOR = new Color(0xE0, 0xE0, 0xE0);
  private static final Color ACCENT_COLOR = new Color(0x00, 0xD2, 0xD3);
  private static final Color FACT_COLOR = new Color(0xFF, 0xD9, 0x3D);

  private final int totalElements;
  private final String funFact;
  private final List<String> debugLines;
  private int startFrameCount = -1;
  private int frozenFrameCount = -1;

  public StatsOverlay(int totalElements, int funFactIndex, List<String> debugLines) {
    this.totalElements = totalElements;
    this.funFact = FUN_FACTS.get(funFactIndex % FUN_FACTS.size());
    this.debugLines = debugLines != null ? debugLines : List.of();
  }

  public void startCounting(int framesWritten) {
    this.startFrameCount = framesWritten;
  }

  public void freeze(int framesWritten) {
    this.frozenFrameCount = framesWritten;
  }

  public void render(
      BufferedImage image, int framesWritten, int fps, int arrayAccesses, int comparisons) {
    int displayFrames;
    if (startFrameCount < 0) {
      displayFrames = 0;
    } else if (frozenFrameCount >= 0) {
      displayFrames = frozenFrameCount - startFrameCount;
    } else {
      displayFrames = framesWritten - startFrameCount;
    }
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    int width = image.getWidth();
    int padding = Math.max(20, width / 20);
    int fontSize = Math.max(24, width / 18);
    Font labelFont = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
    Font valueFont = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
    Font factFont = new Font(Font.MONOSPACED, Font.ITALIC, Math.max(20, fontSize * 3 / 4));
    g.setFont(labelFont);
    FontMetrics fm = g.getFontMetrics(labelFont);

    double elapsedSeconds = (double) displayFrames / fps;
    double secsPerElement = totalElements > 0 ? elapsedSeconds / totalElements : 0;

    String[] labels = {"Elements:", "Duration:", "Duration/El:", "Comparisons:", "Array Access:"};
    String[] values = {
      String.valueOf(totalElements),
      formatDuration(elapsedSeconds),
      String.format("%.4fs", secsPerElement),
      String.format("%,d", comparisons),
      String.format("%,d", arrayAccesses)
    };

    int lineHeight = fm.getHeight() + 4;
    int factLines = 2; // +1 blank +1 fact
    int debugLineCount = debugLines.isEmpty() ? 0 : debugLines.size() + 1; // +1 blank separator
    int lines = labels.length + factLines + debugLineCount;
    int boxHeight = padding * 2 + lineHeight * lines;
    int boxWidth = width - padding * 2;
    int boxX = padding;
    int boxY = padding;

    // Background
    g.setColor(BG_COLOR);
    g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);

    // Stats
    int y = boxY + padding + fm.getAscent();
    for (int i = 0; i < labels.length; i++) {
      g.setFont(labelFont);
      g.setColor(TEXT_COLOR);
      g.drawString(labels[i], boxX + padding, y);

      g.setFont(valueFont);
      g.setColor(ACCENT_COLOR);
      int valueWidth = g.getFontMetrics().stringWidth(values[i]);
      g.drawString(values[i], boxX + boxWidth - padding - valueWidth, y);

      y += lineHeight;
    }

    // Fun fact
    y += lineHeight / 2;
    g.setFont(factFont);
    g.setColor(FACT_COLOR);
    drawWrappedText(g, funFact, boxX + padding, y, boxWidth - padding * 2);

    // Debug info
    if (!debugLines.isEmpty()) {
      y += lineHeight + lineHeight / 2;
      Font debugFont = new Font(Font.MONOSPACED, Font.PLAIN, Math.max(14, fontSize / 2));
      g.setFont(debugFont);
      g.setColor(new Color(0x80, 0x80, 0x90));
      for (String line : debugLines) {
        g.drawString(line, boxX + padding, y);
        y += g.getFontMetrics().getHeight() + 2;
      }
    }

    g.dispose();
  }

  private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth) {
    FontMetrics fm = g.getFontMetrics();
    String[] words = text.split(" ");
    StringBuilder line = new StringBuilder();

    for (String word : words) {
      String test = line.isEmpty() ? word : line + " " + word;
      if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
        g.drawString(line.toString(), x, y);
        y += fm.getHeight() + 2;
        line = new StringBuilder(word);
      } else {
        line = new StringBuilder(test);
      }
    }
    if (!line.isEmpty()) {
      g.drawString(line.toString(), x, y);
    }
  }

  private String formatDuration(double seconds) {
    int mins = (int) (seconds / 60);
    double secs = seconds % 60;
    if (mins > 0) {
      return String.format("%dm %.2fs", mins, secs);
    }
    return String.format("%.2fs", secs);
  }
}
