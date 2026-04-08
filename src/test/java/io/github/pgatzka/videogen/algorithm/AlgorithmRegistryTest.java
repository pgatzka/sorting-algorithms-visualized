package io.github.pgatzka.videogen.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlgorithmRegistryTest {

  @Autowired private AlgorithmRegistry registry;

  @Test
  void findsBubbleSortByName() {
    SortingAlgorithm algorithm = registry.getByName("BubbleSort");
    assertThat(algorithm).isInstanceOf(BubbleSortAlgorithm.class);
  }

  @Test
  void findsQuickSortByName() {
    SortingAlgorithm algorithm = registry.getByName("QuickSort");
    assertThat(algorithm).isInstanceOf(QuickSortAlgorithm.class);
  }

  @Test
  void throwsForUnknownAlgorithm() {
    assertThatThrownBy(() -> registry.getByName("UnknownSort"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UnknownSort");
  }

  @Test
  void getAllNamesReturnsBothAlgorithms() {
    assertThat(registry.getAllNames()).containsExactlyInAnyOrder("BubbleSort", "QuickSort");
  }
}
