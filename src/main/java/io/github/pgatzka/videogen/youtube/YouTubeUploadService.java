package io.github.pgatzka.videogen.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YouTubeUploadService {

  private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String UPLOAD_URL =
      "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status";
  private static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";

  private final YouTubeProperties properties;
  private final YouTubeTokenStore tokenStore;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public YouTubeUploadService(YouTubeProperties properties, YouTubeTokenStore tokenStore) {
    this.properties = properties;
    this.tokenStore = tokenStore;
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  public boolean isEnabled() {
    return properties.isEnabled()
        && properties.getClientId() != null
        && !properties.getClientId().isBlank()
        && properties.getRefreshToken() != null
        && !properties.getRefreshToken().isBlank();
  }

  public boolean isConfiguredButNotAuthenticated() {
    return properties.isEnabled()
        && properties.getClientId() != null
        && !properties.getClientId().isBlank()
        && (properties.getRefreshToken() == null || properties.getRefreshToken().isBlank());
  }

  public String uploadVideo(Path videoPath, String title, String description)
      throws IOException, InterruptedException {
    if (!isEnabled()) {
      throw new IllegalStateException("YouTube upload is not configured");
    }

    String accessToken = refreshAccessToken();

    log.info("YouTube: Initiating resumable upload for '{}'", title);
    String uploadUrl = initResumableUpload(accessToken, title, description);

    log.info("YouTube: Uploading video file ({} bytes)...", Files.size(videoPath));
    String videoId = uploadFile(uploadUrl, videoPath);

    log.info("YouTube: Upload complete. videoId={}", videoId);
    return videoId;
  }

  private String refreshAccessToken() throws IOException, InterruptedException {
    String body =
        String.format(
            "grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s",
            properties.getClientId(), properties.getClientSecret(), properties.getRefreshToken());

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = objectMapper.readTree(response.body());

    if (json.has("error")) {
      throw new IOException("Token refresh failed: " + json.get("error_description").asText());
    }

    String accessToken = json.get("access_token").asText();
    String newRefreshToken = json.path("refresh_token").asText(properties.getRefreshToken());
    tokenStore.saveTokens(accessToken, newRefreshToken);
    log.info("YouTube: Access token refreshed");
    return accessToken;
  }

  private String initResumableUpload(String accessToken, String title, String description)
      throws IOException, InterruptedException {
    Map<String, Object> metadata =
        Map.of(
            "snippet",
            Map.of("title", title, "description", description, "categoryId", "28"),
            "status",
            Map.of("privacyStatus", "public", "selfDeclaredMadeForKids", false));

    String body = objectMapper.writeValueAsString(metadata);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(UPLOAD_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("X-Upload-Content-Type", "video/mp4")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new IOException("Upload init failed: " + response.statusCode() + " " + response.body());
    }

    String location = response.headers().firstValue("Location").orElse(null);
    if (location == null) {
      throw new IOException("No upload Location header in response");
    }
    return location;
  }

  private String uploadFile(String uploadUrl, Path videoPath)
      throws IOException, InterruptedException {
    byte[] fileBytes = Files.readAllBytes(videoPath);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(uploadUrl))
            .header("Content-Type", "video/mp4")
            .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200 && response.statusCode() != 201) {
      throw new IOException("Upload failed: " + response.statusCode() + " " + response.body());
    }

    JsonNode json = objectMapper.readTree(response.body());
    return json.get("id").asText();
  }

  public VideoMetrics fetchMetrics(String videoId) throws IOException, InterruptedException {
    String accessToken =
        properties.getAccessToken() != null ? properties.getAccessToken() : refreshAccessToken();

    String url = VIDEOS_URL + "?part=statistics&id=" + videoId;

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = objectMapper.readTree(response.body());

    JsonNode items = json.path("items");
    if (items.isArray() && !items.isEmpty()) {
      JsonNode stats = items.get(0).path("statistics");
      return new VideoMetrics(
          stats.path("viewCount").asLong(0),
          stats.path("likeCount").asLong(0),
          stats.path("commentCount").asLong(0),
          stats.path("favoriteCount").asLong(0));
    }
    return new VideoMetrics(0, 0, 0, 0);
  }

  public record VideoMetrics(long views, long likes, long comments, long favorites) {}
}
