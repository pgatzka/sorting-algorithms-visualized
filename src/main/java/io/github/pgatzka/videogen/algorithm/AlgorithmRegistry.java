package io.github.pgatzka.videogen.algorithm;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlgorithmRegistry {

  private final Map<String, SortingAlgorithm> algorithms;

  public AlgorithmRegistry(List<SortingAlgorithm> algorithmList) {
    this.algorithms =
        algorithmList.stream()
            .collect(Collectors.toMap(SortingAlgorithm::getName, Function.identity()));
    log.info("Registered {} sorting algorithms: {}", algorithms.size(), algorithms.keySet());
  }

  public SortingAlgorithm getByName(String name) {
    SortingAlgorithm algorithm = algorithms.get(name);
    if (algorithm == null) {
      throw new IllegalArgumentException("Unknown algorithm: " + name);
    }
    return algorithm;
  }

  public List<String> getAllNames() {
    return List.copyOf(algorithms.keySet());
  }
}
