package io.github.pgatzka.videogen.visualization;

import java.awt.*;

public enum ColorScheme {
  DEFAULT(
      new Color(0x00, 0xd2, 0xd3),
      new Color(0xff, 0x6b, 0x35),
      new Color(0xff, 0x35, 0x35),
      new Color(0x2e, 0xcc, 0x71)),
  GREYSCALE(
      new Color(0xb0, 0xb0, 0xb0),
      new Color(0xff, 0xff, 0xff),
      new Color(0x60, 0x60, 0x60),
      new Color(0xd0, 0xd0, 0xd0)),
  RAINBOW(null, new Color(0xff, 0x6b, 0x35), new Color(0xff, 0x35, 0x35), null);

  private final Color barDefault;
  private final Color barCompare;
  private final Color barSwapped;
  private final Color barSorted;

  ColorScheme(Color barDefault, Color barCompare, Color barSwapped, Color barSorted) {
    this.barDefault = barDefault;
    this.barCompare = barCompare;
    this.barSwapped = barSwapped;
    this.barSorted = barSorted;
  }

  public Color getBarColor(int value, int maxValue) {
    if (barDefault == null) {
      float hue = (float) value / maxValue;
      return Color.getHSBColor(hue, 0.8f, 0.9f);
    }
    return barDefault;
  }

  public Color getSortedColor(int value, int maxValue) {
    if (barSorted == null) {
      float hue = (float) value / maxValue;
      return Color.getHSBColor(hue, 0.8f, 0.9f);
    }
    return barSorted;
  }

  public Color getCompareColor() {
    return barCompare;
  }

  public Color getSwappedColor() {
    return barSwapped;
  }
}
