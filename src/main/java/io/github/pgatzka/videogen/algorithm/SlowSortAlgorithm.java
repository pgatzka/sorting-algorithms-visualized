package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SlowSort: a multiply-and-surrender algorithm (opposite of divide-and-conquer). Finds the maximum
 * by recursively sorting halves, comparing their maxes, then recursing on everything except the
 * max. Deliberately pessimal.
 */
@Slf4j
@Component
public class SlowSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "SlowSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting SlowSort with {} elements (patience required)", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));
    slowSort(array, 0, array.length - 1, states, sorted);

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("SlowSort completed: {} states captured", states.size());
    return states;
  }

  private void slowSort(int[] array, int i, int j, List<SortingState> states, Set<Integer> sorted) {
    if (i >= j) return;

    int m = (i + j) / 2;
    slowSort(array, i, m, states, sorted);
    slowSort(array, m + 1, j, states, sorted);

    states.add(new SortingState(array, m, j, false, sorted));
    if (array[m] > array[j]) {
      int temp = array[m];
      array[m] = array[j];
      array[j] = temp;
      states.add(new SortingState(array, m, j, true, sorted));
    }
    sorted.add(j);

    slowSort(array, i, j - 1, states, sorted);
  }
}
