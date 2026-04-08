package io.github.pgatzka.videogen.youtube;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "youtube")
public class YouTubeProperties {

  private boolean enabled = false;
  private String clientId;
  private String clientSecret;
  private String refreshToken;
  private String accessToken;
}
