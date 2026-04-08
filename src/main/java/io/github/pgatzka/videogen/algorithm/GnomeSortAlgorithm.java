package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GnomeSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "GnomeSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting GnomeSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    int i = 0;
    while (i < array.length) {
      if (i == 0 || array[i] >= array[i - 1]) {
        if (i > 0) states.add(new SortingState(array, i, i - 1, false, sorted));
        i++;
      } else {
        states.add(new SortingState(array, i, i - 1, false, sorted));
        int temp = array[i];
        array[i] = array[i - 1];
        array[i - 1] = temp;
        states.add(new SortingState(array, i, i - 1, true, sorted));
        i--;
      }
    }

    for (int j = 0; j < array.length; j++) sorted.add(j);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("GnomeSort completed: {} states captured", states.size());
    return states;
  }
}
