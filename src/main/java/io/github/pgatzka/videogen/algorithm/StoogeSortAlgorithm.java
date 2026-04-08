package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * StoogeSort: if first > last, swap them. Then recursively sort the first 2/3, last 2/3, and first
 * 2/3 again. O(n^2.7) — worse than bubble sort. Named after the Three Stooges.
 */
@Slf4j
@Component
public class StoogeSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "StoogeSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting StoogeSort with {} elements (this may take a while)", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));
    stoogeSort(array, 0, array.length - 1, states, sorted);

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("StoogeSort completed: {} states captured", states.size());
    return states;
  }

  private void stoogeSort(
      int[] array, int i, int j, List<SortingState> states, Set<Integer> sorted) {
    states.add(new SortingState(array, i, j, false, sorted));

    if (array[i] > array[j]) {
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
      states.add(new SortingState(array, i, j, true, sorted));
    }

    if (j - i + 1 > 2) {
      int t = (j - i + 1) / 3;
      stoogeSort(array, i, j - t, states, sorted);
      stoogeSort(array, i + t, j, states, sorted);
      stoogeSort(array, i, j - t, states, sorted);
    }
  }
}
