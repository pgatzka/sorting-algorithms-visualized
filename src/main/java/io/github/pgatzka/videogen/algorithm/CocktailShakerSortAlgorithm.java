package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CocktailShakerSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "CocktailShakerSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting CocktailShakerSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    int start = 0;
    int end = array.length - 1;
    boolean swapped = true;

    while (swapped) {
      swapped = false;

      // Forward pass
      for (int i = start; i < end; i++) {
        states.add(new SortingState(array, i, i + 1, false, sorted));
        if (array[i] > array[i + 1]) {
          int temp = array[i];
          array[i] = array[i + 1];
          array[i + 1] = temp;
          swapped = true;
          states.add(new SortingState(array, i, i + 1, true, sorted));
        }
      }
      sorted.add(end);
      end--;

      if (!swapped) break;
      swapped = false;

      // Backward pass
      for (int i = end; i > start; i--) {
        states.add(new SortingState(array, i, i - 1, false, sorted));
        if (array[i] < array[i - 1]) {
          int temp = array[i];
          array[i] = array[i - 1];
          array[i - 1] = temp;
          swapped = true;
          states.add(new SortingState(array, i, i - 1, true, sorted));
        }
      }
      sorted.add(start);
      start++;
    }

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("CocktailShakerSort completed: {} states captured", states.size());
    return states;
  }
}
