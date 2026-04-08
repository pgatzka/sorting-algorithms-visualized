package io.github.pgatzka.videogen.encoding;

import org.springframework.stereotype.Component;

@Component
public class FfmpegEncoderFactory {

  public FfmpegEncoder create() {
    return new FfmpegEncoder();
  }
}
