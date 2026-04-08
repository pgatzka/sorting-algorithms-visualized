package io.github.pgatzka.videogen.youtube;

import io.github.pgatzka.videogen.job.VideoJobEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CaptionGenerator {

  private static final List<String> HASHTAGS =
      List.of(
          "#sorting",
          "#algorithm",
          "#coding",
          "#programming",
          "#satisfying",
          "#visualization",
          "#computerscience",
          "#tech",
          "#shorts",
          "#oddlysatisfying",
          "#developer",
          "#sortingalgorithm");

  private final Random random = new Random();

  public String generateTitle(VideoJobEntity job) {
    String algo = job.getAlgorithm();
    String viz = job.getVisualization();
    int elements = job.getElementCount();
    boolean isVs = job.getSecondAlgorithm() != null && !job.getSecondAlgorithm().isBlank();

    List<String> titles;
    if (isVs) {
      String algo2 = job.getSecondAlgorithm();
      titles =
          List.of(
              algo + " vs " + algo2 + " | Who Sorts " + elements + " Elements Faster?",
              "Sorting Race: " + algo + " vs " + algo2 + " | " + elements + " Elements",
              algo + " vs " + algo2 + " Showdown | " + viz + " Visualization",
              "Which Is Faster? " + algo + " vs " + algo2 + " | " + elements + " Elements");
    } else {
      titles =
          List.of(
              algo + " Sorting " + elements + " Elements | " + viz + " Visualization",
              algo + " on " + elements + " Elements | " + viz,
              "Satisfying " + algo + " Sort | " + elements + " Elements | " + viz,
              "Watch " + algo + " Sort " + elements + " Elements",
              algo + " Algorithm Visualized | " + viz + " | " + elements + " Elements",
              algo + " vs Chaos: Sorting " + elements + " Elements");
    }
    return titles.get(random.nextInt(titles.size()));
  }

  public String generateDescription(VideoJobEntity job) {
    StringBuilder desc = new StringBuilder();

    boolean isVs = job.getSecondAlgorithm() != null && !job.getSecondAlgorithm().isBlank();
    if (isVs) {
      desc.append(job.getAlgorithm())
          .append(" vs ")
          .append(job.getSecondAlgorithm())
          .append(" racing to sort ")
          .append(job.getElementCount())
          .append(" elements using the ")
          .append(job.getVisualization())
          .append(" visualization.\n\n");
    } else {
      desc.append(job.getAlgorithm())
          .append(" sorting ")
          .append(job.getElementCount())
          .append(" elements visualized as ")
          .append(job.getVisualization())
          .append(".\n\n");
    }

    desc.append("Settings: ");
    desc.append(job.getColorScheme().toLowerCase()).append(" colors");
    if (job.isShuffle()) desc.append(", shuffle animation");
    if (job.isGlowEffect()) desc.append(", glow effect");
    if (job.isParticleTrail()) desc.append(", particle trail");
    if (job.isTweening()) desc.append(", smooth tweening");
    if (job.isSpeedRun()) desc.append(", speed run mode");
    desc.append("\n\n");

    List<String> tags = new ArrayList<>(HASHTAGS);
    tags.add("#" + job.getAlgorithm().toLowerCase());
    tags.add("#" + job.getVisualization().toLowerCase());
    if (isVs) tags.add("#" + job.getSecondAlgorithm().toLowerCase());

    Collections.shuffle(tags, random);
    desc.append(String.join(" ", tags.subList(0, Math.min(tags.size(), 8))));

    return desc.toString();
  }
}
