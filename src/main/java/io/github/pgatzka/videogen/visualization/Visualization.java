package io.github.pgatzka.videogen.visualization;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.awt.image.BufferedImage;

public interface Visualization {

  String getName();

  BufferedImage renderFrame(SortingState state, int width, int height, ColorScheme colorScheme);
}
