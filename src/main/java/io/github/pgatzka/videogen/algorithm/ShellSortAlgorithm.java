package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShellSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "ShellSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting ShellSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();
    int n = array.length;

    states.add(new SortingState(array, -1, -1, false, sorted));

    for (int gap = n / 2; gap > 0; gap /= 2) {
      for (int i = gap; i < n; i++) {
        int temp = array[i];
        int j = i;
        while (j >= gap) {
          states.add(new SortingState(array, j, j - gap, false, sorted));
          if (array[j - gap] > temp) {
            array[j] = array[j - gap];
            states.add(new SortingState(array, j, j - gap, true, sorted));
            j -= gap;
          } else {
            break;
          }
        }
        array[j] = temp;
      }
    }

    for (int i = 0; i < n; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("ShellSort completed: {} states captured", states.size());
    return states;
  }
}
