package io.github.pgatzka.videogen.youtube;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class YouTubeTokenStore {

  private final Path tokenFile;

  private static Path resolveTokenFile() {
    Path dockerPath = Path.of("/app/tokens/youtube-tokens.properties");
    if (dockerPath.getParent().toFile().isDirectory()) {
      return dockerPath;
    }
    return Path.of("youtube-tokens.properties");
  }

  private final YouTubeProperties youTubeProperties;

  public YouTubeTokenStore(YouTubeProperties youTubeProperties) {
    this.youTubeProperties = youTubeProperties;
    this.tokenFile = resolveTokenFile();
    loadTokens();
  }

  public void saveTokens(String accessToken, String refreshToken) {
    youTubeProperties.setAccessToken(accessToken);
    youTubeProperties.setRefreshToken(refreshToken);

    Properties props = new Properties();
    props.setProperty("access-token", accessToken);
    props.setProperty("refresh-token", refreshToken);

    try (var out = Files.newOutputStream(tokenFile)) {
      props.store(out, "YouTube OAuth tokens - DO NOT COMMIT");
      log.info("YouTube tokens saved to {}", tokenFile.toAbsolutePath());
    } catch (IOException e) {
      log.error("Failed to save YouTube tokens", e);
    }
  }

  private void loadTokens() {
    if (!Files.exists(tokenFile)) {
      return;
    }

    Properties props = new Properties();
    try (var in = Files.newInputStream(tokenFile)) {
      props.load(in);
      String refreshToken = props.getProperty("refresh-token");
      String accessToken = props.getProperty("access-token");

      if (refreshToken != null && !refreshToken.isBlank()) {
        youTubeProperties.setRefreshToken(refreshToken);
        log.info("YouTube refresh token loaded from {}", tokenFile.toAbsolutePath());
      }
      if (accessToken != null && !accessToken.isBlank()) {
        youTubeProperties.setAccessToken(accessToken);
      }
    } catch (IOException e) {
      log.warn("Failed to load YouTube tokens from {}", tokenFile, e);
    }
  }
}
