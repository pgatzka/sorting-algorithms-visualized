package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** PancakeSort: repeatedly finds the max, flips it to the top, then flips it into place. */
@Slf4j
@Component
public class PancakeSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "PancakeSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting PancakeSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    for (int size = array.length; size > 1; size--) {
      // Find index of max in array[0..size-1]
      int maxIdx = 0;
      for (int i = 1; i < size; i++) {
        states.add(new SortingState(array, maxIdx, i, false, sorted));
        if (array[i] > array[maxIdx]) {
          maxIdx = i;
        }
      }

      if (maxIdx != size - 1) {
        // Flip max to top
        if (maxIdx > 0) {
          flip(array, maxIdx, states, sorted);
        }
        // Flip to correct position
        flip(array, size - 1, states, sorted);
      }
      sorted.add(size - 1);
    }

    sorted.add(0);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("PancakeSort completed: {} states captured", states.size());
    return states;
  }

  private void flip(int[] array, int end, List<SortingState> states, Set<Integer> sorted) {
    int start = 0;
    while (start < end) {
      states.add(new SortingState(array, start, end, false, sorted));
      int temp = array[start];
      array[start] = array[end];
      array[end] = temp;
      states.add(new SortingState(array, start, end, true, sorted));
      start++;
      end--;
    }
  }
}
