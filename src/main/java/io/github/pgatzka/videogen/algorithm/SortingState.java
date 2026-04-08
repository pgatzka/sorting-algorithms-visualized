package io.github.pgatzka.videogen.algorithm;

import java.util.Set;

public record SortingState(
    int[] array, int compareIdx1, int compareIdx2, boolean swapped, Set<Integer> sorted) {

  public SortingState {
    array = array.clone();
    sorted = Set.copyOf(sorted);
  }
}
