package io.github.pgatzka.videogen.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pgatzka.ApplicationProperties;
import io.github.pgatzka.videogen.algorithm.AlgorithmRegistry;
import io.github.pgatzka.videogen.encoding.FfmpegEncoderFactory;
import io.github.pgatzka.videogen.job.VideoJobService;
import io.github.pgatzka.videogen.visualization.VisualizationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class VideoGeneratorViewTest {

  @Autowired private VideoJobService jobService;
  @Autowired private AlgorithmRegistry algorithmRegistry;
  @Autowired private VisualizationRegistry visualizationRegistry;
  @Autowired private ApplicationProperties properties;
  @MockitoBean private FfmpegEncoderFactory encoderFactory;

  @Test
  void viewCanBeConstructed() {
    VideoGeneratorView view =
        new VideoGeneratorView(jobService, algorithmRegistry, visualizationRegistry, properties);
    assertThat(view).isNotNull();
    assertThat(view.getChildren().count()).isGreaterThan(0);
  }

  @Test
  void viewContainsGrid() {
    VideoGeneratorView view =
        new VideoGeneratorView(jobService, algorithmRegistry, visualizationRegistry, properties);
    boolean hasGrid =
        view.getChildren()
            .anyMatch(component -> component instanceof com.vaadin.flow.component.grid.Grid);
    assertThat(hasGrid).isTrue();
  }
}
