package io.github.pgatzka.videogen.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VideoGenPropertiesTest {

  @Autowired private VideoGenProperties properties;

  @Test
  void propertiesAreLoaded() {
    assertThat(properties).isNotNull();
  }

  @Test
  void defaultValuesAreCorrect() {
    assertThat(properties.getOutputDir()).isEqualTo("./test-videos");
    assertThat(properties.getFfmpegPath()).isEqualTo("ffmpeg");
    assertThat(properties.getDefaultWidth()).isEqualTo(1080);
    assertThat(properties.getDefaultHeight()).isEqualTo(1920);
    assertThat(properties.getDefaultFps()).isEqualTo(60);
    assertThat(properties.getDefaultElementCount()).isEqualTo(50);
    assertThat(properties.getDefaultFramesPerStep()).isEqualTo(3);
  }
}
