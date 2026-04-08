package io.github.pgatzka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "videogen")
public class ApplicationProperties {

  private String outputDir = "./videos";
  private String ffmpegPath = "ffmpeg";
  private int defaultWidth = 1080;
  private int defaultHeight = 1920;
  private int defaultFps = 60;
  private int defaultElementCount = 50;
  private int defaultFramesPerStep = 3;
  private boolean defaultShuffle = true;
}
