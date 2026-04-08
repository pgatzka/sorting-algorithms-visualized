package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SelectionSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "SelectionSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting SelectionSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    for (int i = 0; i < array.length - 1; i++) {
      int minIdx = i;
      for (int j = i + 1; j < array.length; j++) {
        states.add(new SortingState(array, minIdx, j, false, sorted));
        if (array[j] < array[minIdx]) {
          minIdx = j;
        }
      }
      if (minIdx != i) {
        int temp = array[i];
        array[i] = array[minIdx];
        array[minIdx] = temp;
        states.add(new SortingState(array, i, minIdx, true, sorted));
      }
      sorted.add(i);
    }

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("SelectionSort completed: {} states captured", states.size());
    return states;
  }
}
