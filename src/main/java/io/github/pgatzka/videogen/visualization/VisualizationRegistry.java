package io.github.pgatzka.videogen.visualization;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VisualizationRegistry {

  private final Map<String, Visualization> visualizations;

  public VisualizationRegistry(List<Visualization> visualizationList) {
    this.visualizations =
        visualizationList.stream()
            .collect(Collectors.toMap(Visualization::getName, Function.identity()));
    log.info("Registered {} visualizations: {}", visualizations.size(), visualizations.keySet());
  }

  public Visualization getByName(String name) {
    Visualization visualization = visualizations.get(name);
    if (visualization == null) {
      throw new IllegalArgumentException("Unknown visualization: " + name);
    }
    return visualization;
  }

  public List<String> getAllNames() {
    return List.copyOf(visualizations.keySet());
  }
}
