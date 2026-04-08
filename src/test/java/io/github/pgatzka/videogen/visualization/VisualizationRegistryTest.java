package io.github.pgatzka.videogen.visualization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VisualizationRegistryTest {

  @Autowired private VisualizationRegistry registry;

  @Test
  void findsBarChartByName() {
    Visualization visualization = registry.getByName("BarChart");
    assertThat(visualization).isInstanceOf(BarChartVisualization.class);
  }

  @Test
  void throwsForUnknownVisualization() {
    assertThatThrownBy(() -> registry.getByName("Unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown");
  }

  @Test
  void getAllNamesReturnsBarChart() {
    assertThat(registry.getAllNames()).contains("BarChart");
  }
}
