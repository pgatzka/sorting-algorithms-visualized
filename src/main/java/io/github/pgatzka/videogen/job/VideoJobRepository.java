package io.github.pgatzka.videogen.job;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoJobRepository extends JpaRepository<VideoJobEntity, UUID> {

  List<VideoJobEntity> findAllByOrderByCreatedAtDesc();

  List<VideoJobEntity> findByYoutubeVideoIdIsNotNullOrderByCreatedAtDesc();

  List<VideoJobEntity> findByYoutubeVideoIdIsNotNullOrderByYoutubeViewsDesc();

  List<VideoJobEntity> findByYoutubeVideoIdIsNotNullOrderByYoutubeLikesDesc();
}
