package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.image.BufferedImage;
import java.util.Set;

public class TweeningRenderer {

  public BufferedImage renderInterpolated(
      Visualization visualization,
      SortingState previous,
      SortingState current,
      float t,
      int width,
      int height,
      ColorScheme colorScheme) {

    int[] prevArray = previous.array();
    int[] currArray = current.array();
    int n = currArray.length;
    int[] interpolated = new int[n];

    for (int i = 0; i < n; i++) {
      interpolated[i] = Math.round(prevArray[i] + t * (currArray[i] - prevArray[i]));
    }

    // At t=1, use the current state's metadata; otherwise neutral
    int compareIdx1 = t >= 0.5f ? current.compareIdx1() : -1;
    int compareIdx2 = t >= 0.5f ? current.compareIdx2() : -1;
    boolean swapped = t >= 0.5f && current.swapped();
    Set<Integer> sorted = t >= 1.0f ? current.sorted() : previous.sorted();

    SortingState tweened =
        new SortingState(interpolated, compareIdx1, compareIdx2, swapped, sorted);
    return visualization.renderFrame(tweened, width, height, colorScheme);
  }
}
