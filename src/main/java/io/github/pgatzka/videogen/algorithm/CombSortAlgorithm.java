package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CombSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "CombSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting CombSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();
    int n = array.length;

    states.add(new SortingState(array, -1, -1, false, sorted));

    int gap = n;
    boolean swapped = true;

    while (gap > 1 || swapped) {
      gap = Math.max(1, (int) (gap / 1.3));
      swapped = false;

      for (int i = 0; i + gap < n; i++) {
        states.add(new SortingState(array, i, i + gap, false, sorted));
        if (array[i] > array[i + gap]) {
          int temp = array[i];
          array[i] = array[i + gap];
          array[i + gap] = temp;
          swapped = true;
          states.add(new SortingState(array, i, i + gap, true, sorted));
        }
      }
    }

    for (int i = 0; i < n; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("CombSort completed: {} states captured", states.size());
    return states;
  }
}
