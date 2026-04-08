package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * StalinSort: iterates through the array and "removes" any element that is out of order by moving
 * it to the end. Repeats until sorted. Comically authoritarian.
 */
@Slf4j
@Component
public class StalinSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "StalinSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting StalinSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < array.length - 1; i++) {
        states.add(new SortingState(array, i, i + 1, false, sorted));
        if (array[i] > array[i + 1]) {
          // "Deport" the offender to the end
          int offender = array[i + 1];
          System.arraycopy(array, i + 2, array, i + 1, array.length - i - 2);
          array[array.length - 1] = offender;
          states.add(new SortingState(array, i, array.length - 1, true, sorted));
          changed = true;
        }
      }
    }

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("StalinSort completed: {} states captured", states.size());
    return states;
  }
}
