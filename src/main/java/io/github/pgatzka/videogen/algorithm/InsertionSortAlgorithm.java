package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InsertionSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "InsertionSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting InsertionSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));
    sorted.add(0);

    for (int i = 1; i < array.length; i++) {
      int key = array[i];
      int j = i - 1;

      while (j >= 0) {
        states.add(new SortingState(array, j, j + 1, false, sorted));
        if (array[j] > key) {
          array[j + 1] = array[j];
          states.add(new SortingState(array, j, j + 1, true, sorted));
          j--;
        } else {
          break;
        }
      }
      array[j + 1] = key;
      sorted.add(i);
    }

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("InsertionSort completed: {} states captured", states.size());
    return states;
  }
}
