package io.github.pgatzka.videogen.encoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FfmpegEncoderTest {

  @TempDir Path tempDir;

  @BeforeAll
  static void checkFfmpegAvailable() {
    assumeThat(isFfmpegAvailable()).as("FFmpeg must be available on PATH").isTrue();
  }

  @Test
  void encodesSolidColorFrames() throws Exception {
    Path output = tempDir.resolve("solid.mp4");

    try (FfmpegEncoder encoder = new FfmpegEncoder()) {
      encoder.start(output, 320, 240, 30, "ffmpeg");

      BufferedImage frame = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
      Graphics2D g = frame.createGraphics();
      g.setColor(Color.BLUE);
      g.fillRect(0, 0, 320, 240);
      g.dispose();

      for (int i = 0; i < 30; i++) {
        encoder.writeFrame(frame);
      }
    }

    assertThat(Files.exists(output)).isTrue();
    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void encodesVaryingFrames() throws Exception {
    Path output = tempDir.resolve("varying.mp4");

    try (FfmpegEncoder encoder = new FfmpegEncoder()) {
      encoder.start(output, 320, 240, 30, "ffmpeg");

      for (int i = 0; i < 30; i++) {
        BufferedImage frame = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = frame.createGraphics();
        g.setColor(new Color(i * 8, 0, 255 - i * 8));
        g.fillRect(0, 0, 320, 240);
        g.dispose();
        encoder.writeFrame(frame);
      }
    }

    assertThat(Files.exists(output)).isTrue();
    assertThat(Files.size(output)).isGreaterThan(0);
  }

  @Test
  void finishIsIdempotent() throws Exception {
    Path output = tempDir.resolve("idempotent.mp4");

    FfmpegEncoder encoder = new FfmpegEncoder();
    encoder.start(output, 320, 240, 30, "ffmpeg");

    BufferedImage frame = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
    encoder.writeFrame(frame);

    encoder.finish();
    encoder.finish(); // should not throw
    encoder.close(); // should not throw
  }

  private static boolean isFfmpegAvailable() {
    try {
      Process p = new ProcessBuilder("ffmpeg", "-version").start();
      p.getInputStream().readAllBytes();
      return p.waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }
}
