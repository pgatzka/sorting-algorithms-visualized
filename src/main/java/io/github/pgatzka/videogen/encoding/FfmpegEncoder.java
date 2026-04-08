package io.github.pgatzka.videogen.encoding;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FfmpegEncoder implements AutoCloseable {

  private Process process;
  private OutputStream stdin;
  private Thread stderrThread;
  private boolean finished;

  public void start(Path outputPath, int width, int height, int fps, String ffmpegPath)
      throws IOException {
    log.info(
        "Starting FFmpeg encoder: output={}, resolution={}x{}, fps={}, ffmpeg={}",
        outputPath,
        width,
        height,
        fps,
        ffmpegPath);

    ProcessBuilder pb =
        new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-f",
            "rawvideo",
            "-pix_fmt",
            "bgr24",
            "-s",
            width + "x" + height,
            "-r",
            String.valueOf(fps),
            "-i",
            "pipe:0",
            "-c:v",
            "libx264",
            "-pix_fmt",
            "yuv420p",
            "-preset",
            "fast",
            "-crf",
            "18",
            "-movflags",
            "+faststart",
            outputPath.toAbsolutePath().toString());

    pb.redirectErrorStream(false);
    process = pb.start();
    stdin = process.getOutputStream();

    stderrThread =
        new Thread(
            () -> {
              try (var reader = new java.io.BufferedInputStream(process.getErrorStream())) {
                byte[] buffer = new byte[4096];
                int len;
                StringBuilder line = new StringBuilder();
                while ((len = reader.read(buffer)) != -1) {
                  String chunk = new String(buffer, 0, len);
                  line.append(chunk);
                  int newlineIdx;
                  while ((newlineIdx = line.indexOf("\n")) != -1) {
                    String logLine = line.substring(0, newlineIdx).trim();
                    if (!logLine.isEmpty()) {
                      log.debug("FFmpeg: {}", logLine);
                    }
                    line.delete(0, newlineIdx + 1);
                  }
                }
                if (!line.isEmpty()) {
                  log.debug("FFmpeg: {}", line.toString().trim());
                }
              } catch (IOException e) {
                log.warn("Error reading FFmpeg stderr", e);
              }
            },
            "ffmpeg-stderr-reader");
    stderrThread.setDaemon(true);
    stderrThread.start();

    log.info("FFmpeg process started (pid={})", process.pid());
  }

  public void writeFrame(BufferedImage frame) throws IOException {
    if (frame.getType() != BufferedImage.TYPE_3BYTE_BGR) {
      throw new IllegalArgumentException(
          "Expected TYPE_3BYTE_BGR image but got type " + frame.getType());
    }
    byte[] data = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
    stdin.write(data);
  }

  public void finish() throws IOException, InterruptedException {
    if (finished) {
      return;
    }
    finished = true;

    log.info("Finishing FFmpeg encoding...");

    if (stdin != null) {
      stdin.close();
    }

    if (process != null) {
      boolean exited = process.waitFor(5, TimeUnit.MINUTES);
      if (!exited) {
        log.error("FFmpeg process did not exit within 5 minutes, destroying");
        process.destroyForcibly();
        throw new IOException("FFmpeg process timed out");
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new IOException("FFmpeg exited with code " + exitCode);
      }
      log.info("FFmpeg encoding completed successfully");
    }

    if (stderrThread != null) {
      stderrThread.join(5000);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      finish();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while closing FFmpeg encoder", e);
    }
  }
}
