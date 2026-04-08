package io.github.pgatzka.videogen.algorithm;

import java.util.List;

public interface SortingAlgorithm {

  String getName();

  List<SortingState> sort(int[] array);
}
