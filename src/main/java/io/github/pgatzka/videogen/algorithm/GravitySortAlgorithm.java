package io.github.pgatzka.videogen.algorithm;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * GravitySort (BeadSort): simulates beads falling under gravity on vertical rods. Only works with
 * positive integers. Visually satisfying — values "drop" into place.
 */
@Slf4j
@Component
public class GravitySortAlgorithm implements SortingAlgorithm {

  @Override
  public String getName() {
    return "GravitySort";
  }

  @Override
  public List<SortingState> sort(int[] input) {
    log.info("Starting GravitySort with {} elements", input.length);
    int[] array = input.clone();
    List<SortingState> states = new ArrayList<>();
    Set<Integer> sorted = new HashSet<>();
    int n = array.length;

    states.add(new SortingState(array, -1, -1, false, sorted));

    int max = 1;
    for (int val : array) {
      if (val > max) max = val;
    }

    // Simulate beads dropping column by column
    int[][] grid = new int[n][max];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < array[i]; j++) {
        grid[i][j] = 1;
      }
    }

    // Drop beads
    for (int col = 0; col < max; col++) {
      int beads = 0;
      for (int row = 0; row < n; row++) {
        beads += grid[row][col];
        grid[row][col] = 0;
      }
      for (int row = n - 1; row >= n - beads; row--) {
        grid[row][col] = 1;
      }

      // Update array from grid and capture state
      for (int i = 0; i < n; i++) {
        int sum = 0;
        for (int j = 0; j < max; j++) sum += grid[i][j];
        if (array[i] != sum) {
          int oldVal = array[i];
          array[i] = sum;
        }
      }
      // Show which column was processed
      int showIdx = Math.min(col, n - 1);
      states.add(new SortingState(array, showIdx, -1, true, sorted));
    }

    for (int i = 0; i < n; i++) sorted.add(i);
    states.add(new SortingState(array, -1, -1, false, sorted));

    log.info("GravitySort completed: {} states captured", states.size());
    return states;
  }
}
