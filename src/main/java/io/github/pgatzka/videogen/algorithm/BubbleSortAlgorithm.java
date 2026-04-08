package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BubbleSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "BubbleSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting BubbleSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    int n = array.length;
    for (int i = 0; i < n - 1; i++) {
      boolean anySwapped = false;
      for (int j = 0; j < n - 1 - i; j++) {
        states.add(new SortingState(array, j, j + 1, false, sorted));

        if (array[j] > array[j + 1]) {
          int temp = array[j];
          array[j] = array[j + 1];
          array[j + 1] = temp;
          anySwapped = true;
          states.add(new SortingState(array, j, j + 1, true, sorted));
        }
      }
      sorted.add(n - 1 - i);
      if (!anySwapped) {
        for (int k = 0; k < n - i; k++) {
          sorted.add(k);
        }
        break;
      }
    }
    sorted.add(0);

    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("BubbleSort completed: {} states captured", states.size());
    return states;
  }
}
