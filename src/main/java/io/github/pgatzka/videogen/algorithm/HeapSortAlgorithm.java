package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HeapSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "HeapSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting HeapSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();
    int n = array.length;

    states.add(new SortingState(array, -1, -1, false, sorted));

    // Build max heap
    for (int i = n / 2 - 1; i >= 0; i--) {
      heapify(array, n, i, states, sorted);
    }

    // Extract elements from heap
    for (int i = n - 1; i > 0; i--) {
      states.add(new SortingState(array, 0, i, false, sorted));
      int temp = array[0];
      array[0] = array[i];
      array[i] = temp;
      states.add(new SortingState(array, 0, i, true, sorted));
      sorted.add(i);
      heapify(array, i, 0, states, sorted);
    }

    sorted.add(0);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("HeapSort completed: {} states captured", states.size());
    return states;
  }

  private void heapify(int[] array, int n, int i, List<SortingState> states, Set<Integer> sorted) {
    int largest = i;
    int left = 2 * i + 1;
    int right = 2 * i + 2;

    if (left < n) {
      states.add(new SortingState(array, largest, left, false, sorted));
      if (array[left] > array[largest]) largest = left;
    }
    if (right < n) {
      states.add(new SortingState(array, largest, right, false, sorted));
      if (array[right] > array[largest]) largest = right;
    }

    if (largest != i) {
      int temp = array[i];
      array[i] = array[largest];
      array[largest] = temp;
      states.add(new SortingState(array, i, largest, true, sorted));
      heapify(array, n, largest, states, sorted);
    }
  }
}
