package io.github.pgatzka.videogen.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YouTubeDeviceAuthService {

  private static final String DEVICE_CODE_URL = "https://oauth2.googleapis.com/device/code";
  private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String SCOPES =
      "https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube.readonly";

  private final YouTubeProperties properties;
  private final YouTubeTokenStore tokenStore;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  private volatile DeviceCodeResponse pendingAuth;

  public YouTubeDeviceAuthService(YouTubeProperties properties, YouTubeTokenStore tokenStore) {
    this.properties = properties;
    this.tokenStore = tokenStore;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  public boolean isConfigured() {
    return properties.getClientId() != null
        && !properties.getClientId().isBlank()
        && properties.getClientSecret() != null
        && !properties.getClientSecret().isBlank();
  }

  public boolean isAuthenticated() {
    return properties.getRefreshToken() != null && !properties.getRefreshToken().isBlank();
  }

  public boolean isPendingAuth() {
    return pendingAuth != null;
  }

  public DeviceCodeResponse startDeviceAuth() throws IOException, InterruptedException {
    if (!isConfigured()) {
      throw new IllegalStateException(
          "YouTube client-id and client-secret must be configured first");
    }

    String body = String.format("client_id=%s&scope=%s", properties.getClientId(), SCOPES);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(DEVICE_CODE_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = objectMapper.readTree(response.body());

    if (json.has("error")) {
      throw new IOException("Device auth failed: " + json.get("error_description").asText());
    }

    pendingAuth = new DeviceCodeResponse();
    pendingAuth.setDeviceCode(json.get("device_code").asText());
    pendingAuth.setUserCode(json.get("user_code").asText());
    pendingAuth.setVerificationUrl(json.get("verification_url").asText());
    pendingAuth.setInterval(json.get("interval").asInt(5));
    pendingAuth.setExpiresIn(json.get("expires_in").asInt(1800));

    log.info(
        "YouTube device auth started: go to {} and enter code {}",
        pendingAuth.getVerificationUrl(),
        pendingAuth.getUserCode());

    // Start polling in background
    Thread.ofVirtual().name("youtube-device-auth-poll").start(this::pollForToken);

    return pendingAuth;
  }

  private void pollForToken() {
    DeviceCodeResponse auth = pendingAuth;
    if (auth == null) return;

    int maxAttempts = auth.getExpiresIn() / auth.getInterval();
    for (int i = 0; i < maxAttempts; i++) {
      try {
        Thread.sleep(auth.getInterval() * 1000L);

        String body =
            String.format(
                "client_id=%s&client_secret=%s&device_code=%s&grant_type=urn:ietf:params:oauth:grant-type:device_code",
                properties.getClientId(), properties.getClientSecret(), auth.getDeviceCode());

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());

        if (json.has("access_token")) {
          tokenStore.saveTokens(
              json.get("access_token").asText(), json.get("refresh_token").asText());
          pendingAuth = null;
          log.info("YouTube device auth successful! Tokens persisted.");
          return;
        }

        String error = json.path("error").asText("");
        if ("authorization_pending".equals(error)) {
          continue; // User hasn't authorized yet
        } else if ("slow_down".equals(error)) {
          Thread.sleep(5000); // Back off
        } else {
          log.error("YouTube device auth error: {}", error);
          pendingAuth = null;
          return;
        }

      } catch (Exception e) {
        log.error("YouTube device auth polling failed", e);
        pendingAuth = null;
        return;
      }
    }

    log.warn("YouTube device auth expired");
    pendingAuth = null;
  }

  @Data
  public static class DeviceCodeResponse {
    private String deviceCode;
    private String userCode;
    private String verificationUrl;
    private int interval;
    private int expiresIn;
  }
}
