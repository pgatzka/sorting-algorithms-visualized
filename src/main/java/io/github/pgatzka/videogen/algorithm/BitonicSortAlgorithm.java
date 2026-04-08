package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** BitonicSort: builds bitonic sequences and merges them. Pads to power of 2. */
@Slf4j
@Component
public class BitonicSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "BitonicSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting BitonicSort with {} elements", input.length);
    // Pad to next power of 2
    int n = 1;
    while (n < input.length) n <<= 1;
    int[] array = new int[n];
    System.arraycopy(input, 0, array, 0, input.length);
    for (int i = input.length; i < n; i++) {
      array[i] = Integer.MAX_VALUE;
    }

    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    // Only capture states for original indices
    states.add(new SortingState(trimmed(array, input.length), -1, -1, false, sorted));
    bitonicSort(array, 0, n, true, states, sorted, input.length);

    for (int i = 0; i < input.length; i++) sorted.add(i);
    states.add(new SortingState(trimmed(array, input.length), -1, -1, false, sorted));

    log.info("BitonicSort completed: {} states captured", states.size());
    return states;
  }

  private void bitonicSort(
      int[] array,
      int low,
      int count,
      boolean ascending,
      List<SortingState> states,
      Set<Integer> sorted,
      int origLen) {
    if (count > 1) {
      int half = count / 2;
      bitonicSort(array, low, half, true, states, sorted, origLen);
      bitonicSort(array, low + half, half, false, states, sorted, origLen);
      bitonicMerge(array, low, count, ascending, states, sorted, origLen);
    }
  }

  private void bitonicMerge(
      int[] array,
      int low,
      int count,
      boolean ascending,
      List<SortingState> states,
      Set<Integer> sorted,
      int origLen) {
    if (count > 1) {
      int half = count / 2;
      for (int i = low; i < low + half; i++) {
        int j = i + half;
        if (i < origLen && j < origLen) {
          states.add(new SortingState(trimmed(array, origLen), i, j, false, sorted));
        }
        if ((ascending && array[i] > array[j]) || (!ascending && array[i] < array[j])) {
          int temp = array[i];
          array[i] = array[j];
          array[j] = temp;
          if (i < origLen && j < origLen) {
            states.add(new SortingState(trimmed(array, origLen), i, j, true, sorted));
          }
        }
      }
      bitonicMerge(array, low, half, ascending, states, sorted, origLen);
      bitonicMerge(array, low + half, half, ascending, states, sorted, origLen);
    }
  }

  private int[] trimmed(int[] array, int len) {
    return Arrays.copyOf(array, len);
  }
}
