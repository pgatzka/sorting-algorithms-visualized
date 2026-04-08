package io.github.pgatzka.videogen.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuickSortAlgorithmTest {

  private final QuickSortAlgorithm algorithm = new QuickSortAlgorithm();

  @Test
  void nameIsQuickSort() {
    assertThat(algorithm.getName()).isEqualTo("QuickSort");
  }

  @Test
  void sortsCorrectly() {
    int[] input = {5, 3, 8, 1, 2};
    List<SortingState> states = algorithm.sort(input);

    int[] expected = {5, 3, 8, 1, 2};
    Arrays.sort(expected);

    assertThat(states.getLast().array()).isEqualTo(expected);
  }

  @Test
  void doesNotMutateInput() {
    int[] input = {5, 3, 8, 1, 2};
    int[] copy = input.clone();
    algorithm.sort(input);
    assertThat(input).isEqualTo(copy);
  }

  @Test
  void capturesIntermediateStates() {
    int[] input = {5, 3, 8, 1, 2};
    List<SortingState> states = algorithm.sort(input);
    assertThat(states).hasSizeGreaterThan(2);
  }

  @Test
  void firstStateIsOriginalArray() {
    int[] input = {5, 3, 8, 1, 2};
    List<SortingState> states = algorithm.sort(input);
    assertThat(states.getFirst().array()).isEqualTo(input);
  }

  @Test
  void lastStateHasAllIndicesSorted() {
    int[] input = {5, 3, 8, 1, 2};
    List<SortingState> states = algorithm.sort(input);
    assertThat(states.getLast().sorted()).hasSize(input.length);
  }

  @Test
  void statesHaveValidCompareIndices() {
    int[] input = {5, 3, 8, 1, 2};
    List<SortingState> states = algorithm.sort(input);
    for (SortingState state : states) {
      assertThat(state.compareIdx1()).isGreaterThanOrEqualTo(-1);
      assertThat(state.compareIdx1()).isLessThan(input.length);
      assertThat(state.compareIdx2()).isGreaterThanOrEqualTo(-1);
      assertThat(state.compareIdx2()).isLessThan(input.length);
    }
  }

  @Test
  void singleElement() {
    int[] input = {42};
    List<SortingState> states = algorithm.sort(input);
    assertThat(states).isNotEmpty();
    assertThat(states.getLast().array()).isEqualTo(new int[] {42});
  }

  @Test
  void twoElements() {
    int[] input = {2, 1};
    List<SortingState> states = algorithm.sort(input);
    assertThat(states.getLast().array()).isEqualTo(new int[] {1, 2});
  }

  @Test
  void handlesLargerArray() {
    int[] input = {10, 7, 8, 9, 1, 5, 3, 4, 2, 6};
    List<SortingState> states = algorithm.sort(input);

    int[] expected = input.clone();
    Arrays.sort(expected);
    assertThat(states.getLast().array()).isEqualTo(expected);
  }
}
