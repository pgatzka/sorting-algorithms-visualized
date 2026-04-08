package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuickSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "QuickSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting QuickSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new TreeSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));

    quickSort(array, 0, array.length - 1, states, sorted);

    for (int i = 0; i < array.length; i++) {
      sorted.add(i);
    }
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("QuickSort completed: {} states captured", states.size());
    return states;
  }

  private void quickSort(
      int[] array, int low, int high, List<SortingState> states, Set<Integer> sorted) {
    if (low < high) {
      int pivotIdx = partition(array, low, high, states, sorted);
      sorted.add(pivotIdx);
      states.add(new SortingState(array, pivotIdx, -1, false, sorted));
      quickSort(array, low, pivotIdx - 1, states, sorted);
      quickSort(array, pivotIdx + 1, high, states, sorted);
    } else if (low == high) {
      sorted.add(low);
    }
  }

  private int partition(
      int[] array, int low, int high, List<SortingState> states, Set<Integer> sorted) {
    int pivot = array[high];
    int i = low - 1;

    for (int j = low; j < high; j++) {
      states.add(new SortingState(array, j, high, false, sorted));

      if (array[j] <= pivot) {
        i++;
        if (i != j) {
          int temp = array[i];
          array[i] = array[j];
          array[j] = temp;
          states.add(new SortingState(array, i, j, true, sorted));
        }
      }
    }

    int temp = array[i + 1];
    array[i + 1] = array[high];
    array[high] = temp;
    if (i + 1 != high) {
      states.add(new SortingState(array, i + 1, high, true, sorted));
    }

    return i + 1;
  }
}
