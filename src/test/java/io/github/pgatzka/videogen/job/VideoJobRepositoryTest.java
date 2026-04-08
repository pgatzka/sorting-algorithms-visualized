package io.github.pgatzka.videogen.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VideoJobRepositoryTest {

  @Autowired private VideoJobRepository repository;

  @Test
  void saveAndRetrieveEntity() {
    VideoJobEntity entity = createEntity("BubbleSort", "BarChart");
    VideoJobEntity saved = repository.save(entity);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();

    Optional<VideoJobEntity> found = repository.findById(saved.getId());
    assertThat(found).isPresent();

    VideoJobEntity loaded = found.get();
    assertThat(loaded.getAlgorithm()).isEqualTo("BubbleSort");
    assertThat(loaded.getVisualization()).isEqualTo("BarChart");
    assertThat(loaded.getElementCount()).isEqualTo(50);
    assertThat(loaded.getFps()).isEqualTo(60);
    assertThat(loaded.getWidth()).isEqualTo(1080);
    assertThat(loaded.getHeight()).isEqualTo(1920);
    assertThat(loaded.getFramesPerStep()).isEqualTo(3);
    assertThat(loaded.getStatus()).isEqualTo(VideoJobStatus.QUEUED);
    assertThat(loaded.getProgress()).isZero();
    assertThat(loaded.getOutputPath()).isNull();
    assertThat(loaded.getErrorMessage()).isNull();
  }

  @Test
  void findAllOrderedByCreatedAtDesc() {
    VideoJobEntity first = createEntity("BubbleSort", "BarChart");
    repository.save(first);

    VideoJobEntity second = createEntity("QuickSort", "BarChart");
    repository.save(second);

    List<VideoJobEntity> all = repository.findAllByOrderByCreatedAtDesc();
    assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    // Verify ordering: first result should have createdAt >= second
    assertThat(all.get(0).getCreatedAt()).isAfterOrEqualTo(all.get(1).getCreatedAt());
  }

  @Test
  void statusUpdatePersists() {
    VideoJobEntity entity = createEntity("BubbleSort", "BarChart");
    VideoJobEntity saved = repository.save(entity);

    saved.setStatus(VideoJobStatus.RUNNING);
    saved.setProgress(50);
    repository.save(saved);

    VideoJobEntity reloaded = repository.findById(saved.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(VideoJobStatus.RUNNING);
    assertThat(reloaded.getProgress()).isEqualTo(50);
  }

  private VideoJobEntity createEntity(String algorithm, String visualization) {
    VideoJobEntity entity = new VideoJobEntity();
    entity.setAlgorithm(algorithm);
    entity.setVisualization(visualization);
    entity.setElementCount(50);
    entity.setFps(60);
    entity.setWidth(1080);
    entity.setHeight(1920);
    entity.setFramesPerStep(3);
    return entity;
  }
}
