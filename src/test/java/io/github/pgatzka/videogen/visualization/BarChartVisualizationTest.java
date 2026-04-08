package io.github.pgatzka.videogen.visualization;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.image.BufferedImage;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BarChartVisualizationTest {

  private final BarChartVisualization visualization = new BarChartVisualization();

  @Test
  void nameIsBarChart() {
    assertThat(visualization.getName()).isEqualTo("BarChart");
  }

  @Test
  void rendersCorrectDimensions() {
    SortingState state = new SortingState(new int[] {3, 1, 2}, -1, -1, false, Set.of());
    BufferedImage image = visualization.renderFrame(state, 1080, 1920, ColorScheme.DEFAULT);
    assertThat(image.getWidth()).isEqualTo(1080);
    assertThat(image.getHeight()).isEqualTo(1920);
  }

  @Test
  void imageTypeIs3ByteBgr() {
    SortingState state = new SortingState(new int[] {3, 1, 2}, -1, -1, false, Set.of());
    BufferedImage image = visualization.renderFrame(state, 1080, 1920, ColorScheme.DEFAULT);
    assertThat(image.getType()).isEqualTo(BufferedImage.TYPE_3BYTE_BGR);
  }

  @Test
  void imageIsNotBlank() {
    SortingState state = new SortingState(new int[] {5, 3, 8, 1, 2}, -1, -1, false, Set.of());
    BufferedImage image = visualization.renderFrame(state, 1080, 1920, ColorScheme.DEFAULT);

    // Check that at least some pixels differ from the background color (0x1a1a2e)
    int bgRgb = 0xFF1a1a2e;
    boolean hasNonBgPixel = false;
    for (int y = 0; y < image.getHeight() && !hasNonBgPixel; y += 10) {
      for (int x = 0; x < image.getWidth() && !hasNonBgPixel; x += 10) {
        if (image.getRGB(x, y) != bgRgb) {
          hasNonBgPixel = true;
        }
      }
    }
    assertThat(hasNonBgPixel).isTrue();
  }

  @Test
  void differentStatesProduceDifferentImages() {
    SortingState state1 = new SortingState(new int[] {5, 3, 8, 1, 2}, -1, -1, false, Set.of());
    SortingState state2 = new SortingState(new int[] {1, 2, 3, 5, 8}, -1, -1, false, Set.of());
    BufferedImage image1 = visualization.renderFrame(state1, 1080, 1920, ColorScheme.DEFAULT);
    BufferedImage image2 = visualization.renderFrame(state2, 1080, 1920, ColorScheme.DEFAULT);

    boolean differ = false;
    outer:
    for (int y = 0; y < image1.getHeight(); y += 10) {
      for (int x = 0; x < image1.getWidth(); x += 10) {
        if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
          differ = true;
          break outer;
        }
      }
    }
    assertThat(differ).isTrue();
  }

  @Test
  void highlightedBarsHaveDifferentColor() {
    SortingState noHighlight = new SortingState(new int[] {5, 3, 8, 1, 2}, -1, -1, false, Set.of());
    SortingState withHighlight = new SortingState(new int[] {5, 3, 8, 1, 2}, 0, 1, false, Set.of());
    BufferedImage imageNoHL =
        visualization.renderFrame(noHighlight, 1080, 1920, ColorScheme.DEFAULT);
    BufferedImage imageHL =
        visualization.renderFrame(withHighlight, 1080, 1920, ColorScheme.DEFAULT);

    boolean differ = false;
    outer:
    for (int y = 0; y < imageNoHL.getHeight(); y += 5) {
      for (int x = 0; x < imageNoHL.getWidth(); x += 5) {
        if (imageNoHL.getRGB(x, y) != imageHL.getRGB(x, y)) {
          differ = true;
          break outer;
        }
      }
    }
    assertThat(differ).isTrue();
  }

  @Test
  void emptyArrayRendersWithoutError() {
    SortingState state = new SortingState(new int[] {}, -1, -1, false, Set.of());
    BufferedImage image = visualization.renderFrame(state, 1080, 1920, ColorScheme.DEFAULT);
    assertThat(image).isNotNull();
  }
}
