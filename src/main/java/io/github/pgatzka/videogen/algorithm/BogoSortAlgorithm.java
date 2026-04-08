package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BogoSort: randomly shuffles the array until it's sorted. Average: O(n × n!). Capped at 10,000
 * attempts to avoid infinite videos.
 */
@Slf4j
@Component
public class BogoSortAlgorithm implements SortingAlgorithm {

  private static final int MAX_ATTEMPTS = 10_000;

  @Override
  public String getName() {
    return "BogoSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting BogoSort with {} elements (pray)", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();
    Random random = new Random();

    states.add(new SortingState(array, -1, -1, false, sorted));

    int attempts = 0;
    while (!isSorted(array) && attempts < MAX_ATTEMPTS) {
      // Fisher-Yates shuffle
      for (int i = array.length - 1; i > 0; i--) {
        int j = random.nextInt(i + 1);
        if (i != j) {
          states.add(new SortingState(array, i, j, false, sorted));
          int temp = array[i];
          array[i] = array[j];
          array[j] = temp;
          states.add(new SortingState(array, i, j, true, sorted));
        }
      }
      attempts++;
    }

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("BogoSort completed after {} attempts: {} states captured", attempts, states.size());
    return states;
  }

  private boolean isSorted(int[] array) {
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i] > array[i + 1]) return false;
    }
    return true;
  }
}
