package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MergeSortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "MergeSort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting MergeSort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();

    states.add(new SortingState(array, -1, -1, false, sorted));
    mergeSort(array, 0, array.length - 1, states, sorted);

    for (int i = 0; i < array.length; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("MergeSort completed: {} states captured", states.size());
    return states;
  }

  private void mergeSort(
      int[] array, int left, int right, List<SortingState> states, Set<Integer> sorted) {
    if (left < right) {
      int mid = (left + right) / 2;
      mergeSort(array, left, mid, states, sorted);
      mergeSort(array, mid + 1, right, states, sorted);
      merge(array, left, mid, right, states, sorted);
    }
  }

  private void merge(
      int[] array, int left, int mid, int right, List<SortingState> states, Set<Integer> sorted) {
    int[] temp = Arrays.copyOfRange(array, left, right + 1);
    int i = 0;
    int j = mid - left + 1;
    int k = left;

    while (i <= mid - left && j <= right - left) {
      states.add(new SortingState(array, left + i, left + j, false, sorted));
      if (temp[i] <= temp[j]) {
        array[k] = temp[i];
        i++;
      } else {
        array[k] = temp[j];
        j++;
      }
      states.add(new SortingState(array, k, -1, true, sorted));
      k++;
    }

    while (i <= mid - left) {
      array[k] = temp[i];
      states.add(new SortingState(array, k, -1, true, sorted));
      i++;
      k++;
    }

    while (j <= right - left) {
      array[k] = temp[j];
      states.add(new SortingState(array, k, -1, true, sorted));
      j++;
      k++;
    }
  }
}
