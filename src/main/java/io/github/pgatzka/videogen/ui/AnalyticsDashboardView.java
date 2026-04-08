package io.github.pgatzka.videogen.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import io.github.pgatzka.videogen.job.VideoJobEntity;
import io.github.pgatzka.videogen.job.VideoJobRepository;
import io.github.pgatzka.videogen.youtube.MetricsRefreshScheduler;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Route("analytics")
public class AnalyticsDashboardView extends VerticalLayout {

  private final VideoJobRepository repository;
  private final MetricsRefreshScheduler metricsRefreshScheduler;
  private final Span totalVideosLabel = new Span();
  private final Span totalViewsLabel = new Span();
  private final Span totalLikesLabel = new Span();
  private final Span totalCommentsLabel = new Span();
  private final Span avgViewsLabel = new Span();
  private final Grid<VideoJobEntity> topVideosGrid;
  private final Grid<Map.Entry<String, Long>> algorithmRankGrid;
  private final Grid<Map.Entry<String, Long>> visualizationRankGrid;
  private Select<String> sortBySelect;

  public AnalyticsDashboardView(
      VideoJobRepository repository, MetricsRefreshScheduler metricsRefreshScheduler) {
    this.repository = repository;
    this.metricsRefreshScheduler = metricsRefreshScheduler;

    setSizeFull();
    setPadding(true);
    setSpacing(true);

    HorizontalLayout nav = new HorizontalLayout();
    nav.setWidthFull();
    nav.setAlignItems(Alignment.CENTER);
    nav.add(new H2("YouTube Analytics Dashboard"));
    Button refreshMetricsBtn = new Button("Refresh Metrics");
    refreshMetricsBtn.addClickListener(
        e -> {
          refreshMetricsBtn.setEnabled(false);
          refreshMetricsBtn.setText("Refreshing...");
          new Thread(
                  () -> {
                    metricsRefreshScheduler.refreshMetrics();
                    refreshMetricsBtn
                        .getUI()
                        .ifPresent(
                            ui ->
                                ui.access(
                                    () -> {
                                      refreshMetricsBtn.setEnabled(true);
                                      refreshMetricsBtn.setText("Refresh Metrics");
                                      refreshData();
                                      Notification.show("Metrics refreshed");
                                    }));
                  })
              .start();
        });
    Anchor backLink = new Anchor("/", "Back to Generator");
    backLink.getStyle().set("margin-left", "auto");
    nav.add(refreshMetricsBtn, backLink);
    add(nav);

    // Summary cards
    HorizontalLayout summary = new HorizontalLayout();
    summary.setWidthFull();
    summary.add(
        createStatCard("Videos", totalVideosLabel),
        createStatCard("Total Views", totalViewsLabel),
        createStatCard("Total Likes", totalLikesLabel),
        createStatCard("Total Comments", totalCommentsLabel),
        createStatCard("Avg Views", avgViewsLabel));
    add(summary);

    // Sort selector
    sortBySelect = new Select<>();
    sortBySelect.setLabel("Sort by");
    sortBySelect.setItems("Most Views", "Most Likes", "Most Recent");
    sortBySelect.setValue("Most Views");
    sortBySelect.addValueChangeListener(e -> refreshData());
    add(sortBySelect);

    // Top videos grid
    add(new H3("Video Performance"));
    topVideosGrid = new Grid<>();
    topVideosGrid
        .addColumn(
            entity ->
                entity.getAlgorithm()
                    + (entity.getSecondAlgorithm() != null
                        ? " vs " + entity.getSecondAlgorithm()
                        : ""))
        .setHeader("Algorithm");
    topVideosGrid.addColumn(VideoJobEntity::getVisualization).setHeader("Visualization");
    topVideosGrid.addColumn(VideoJobEntity::getColorScheme).setHeader("Colors");
    topVideosGrid.addColumn(VideoJobEntity::getElementCount).setHeader("Elements");
    topVideosGrid.addColumn(entity -> formatNumber(entity.getYoutubeViews())).setHeader("Views");
    topVideosGrid.addColumn(entity -> formatNumber(entity.getYoutubeLikes())).setHeader("Likes");
    topVideosGrid
        .addColumn(entity -> formatNumber(entity.getYoutubeComments()))
        .setHeader("Comments");
    topVideosGrid
        .addColumn(
            entity ->
                entity.getYoutubeViews() != null
                        && entity.getYoutubeViews() > 0
                        && entity.getYoutubeLikes() != null
                    ? String.format(
                        "%.1f%%", entity.getYoutubeLikes() * 100.0 / entity.getYoutubeViews())
                    : "-")
        .setHeader("Like Rate");
    topVideosGrid.addColumn(this::formatFlags).setHeader("Flags");
    topVideosGrid
        .addColumn(entity -> entity.getYoutubeVideoId() != null ? entity.getYoutubeVideoId() : "")
        .setHeader("Video ID");
    topVideosGrid
        .addColumn(
            entity ->
                entity.getCreatedAt() != null
                    ? entity
                        .getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                    : "")
        .setHeader("Date");
    topVideosGrid.setHeight("400px");
    add(topVideosGrid);

    // Rankings side by side
    HorizontalLayout rankings = new HorizontalLayout();
    rankings.setWidthFull();

    VerticalLayout algoRankLayout = new VerticalLayout();
    algoRankLayout.add(new H3("Best Algorithms (by avg views)"));
    algorithmRankGrid = new Grid<>();
    algorithmRankGrid.addColumn(Map.Entry::getKey).setHeader("Algorithm");
    algorithmRankGrid.addColumn(entry -> formatNumber(entry.getValue())).setHeader("Avg Views");
    algorithmRankGrid.setHeight("300px");
    algoRankLayout.add(algorithmRankGrid);

    VerticalLayout vizRankLayout = new VerticalLayout();
    vizRankLayout.add(new H3("Best Visualizations (by avg views)"));
    visualizationRankGrid = new Grid<>();
    visualizationRankGrid.addColumn(Map.Entry::getKey).setHeader("Visualization");
    visualizationRankGrid.addColumn(entry -> formatNumber(entry.getValue())).setHeader("Avg Views");
    visualizationRankGrid.setHeight("300px");
    vizRankLayout.add(visualizationRankGrid);

    rankings.add(algoRankLayout, vizRankLayout);
    add(rankings);

    refreshData();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    UI ui = attachEvent.getUI();
    ui.setPollInterval(30000);
    ui.addPollListener(event -> refreshData());
  }

  private void refreshData() {
    List<VideoJobEntity> uploaded = repository.findByYoutubeVideoIdIsNotNullOrderByCreatedAtDesc();

    long totalViews =
        uploaded.stream()
            .mapToLong(j -> j.getYoutubeViews() != null ? j.getYoutubeViews() : 0)
            .sum();
    long totalLikes =
        uploaded.stream()
            .mapToLong(j -> j.getYoutubeLikes() != null ? j.getYoutubeLikes() : 0)
            .sum();
    long totalComments =
        uploaded.stream()
            .mapToLong(j -> j.getYoutubeComments() != null ? j.getYoutubeComments() : 0)
            .sum();
    long avgViews = uploaded.isEmpty() ? 0 : totalViews / uploaded.size();

    totalVideosLabel.setText(String.valueOf(uploaded.size()));
    totalViewsLabel.setText(formatNumber(totalViews));
    totalLikesLabel.setText(formatNumber(totalLikes));
    totalCommentsLabel.setText(formatNumber(totalComments));
    avgViewsLabel.setText(formatNumber(avgViews));

    List<VideoJobEntity> sorted;
    String sortBy = sortBySelect.getValue();
    if ("Most Likes".equals(sortBy)) {
      sorted =
          uploaded.stream()
              .sorted(
                  Comparator.comparingLong(
                          (VideoJobEntity j) ->
                              j.getYoutubeLikes() != null ? j.getYoutubeLikes() : 0)
                      .reversed())
              .toList();
    } else if ("Most Recent".equals(sortBy)) {
      sorted = uploaded;
    } else {
      sorted =
          uploaded.stream()
              .sorted(
                  Comparator.comparingLong(
                          (VideoJobEntity j) ->
                              j.getYoutubeViews() != null ? j.getYoutubeViews() : 0)
                      .reversed())
              .toList();
    }
    topVideosGrid.setItems(sorted);

    Map<String, Long> algoAvgViews =
        uploaded.stream()
            .collect(
                Collectors.groupingBy(
                    VideoJobEntity::getAlgorithm,
                    Collectors.averagingLong(
                        j -> j.getYoutubeViews() != null ? j.getYoutubeViews() : 0)))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));

    algorithmRankGrid.setItems(
        algoAvgViews.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList());

    Map<String, Long> vizAvgViews =
        uploaded.stream()
            .collect(
                Collectors.groupingBy(
                    VideoJobEntity::getVisualization,
                    Collectors.averagingLong(
                        j -> j.getYoutubeViews() != null ? j.getYoutubeViews() : 0)))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));

    visualizationRankGrid.setItems(
        vizAvgViews.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .toList());
  }

  private VerticalLayout createStatCard(String label, Span valueSpan) {
    VerticalLayout card = new VerticalLayout();
    card.setPadding(true);
    card.getStyle().set("background", "var(--lumo-contrast-5pct)");
    card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    card.setAlignItems(Alignment.CENTER);
    card.setWidth(null);

    Span labelSpan = new Span(label);
    labelSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
    labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

    valueSpan.getStyle().set("font-size", "var(--lumo-font-size-xxl)");
    valueSpan.getStyle().set("font-weight", "bold");
    valueSpan.setText("0");

    card.add(labelSpan, valueSpan);
    return card;
  }

  private String formatNumber(Long value) {
    if (value == null) return "-";
    return formatNumber(value.longValue());
  }

  private String formatNumber(long value) {
    if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
    if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
    return String.valueOf(value);
  }

  private String formatFlags(VideoJobEntity entity) {
    StringBuilder flags = new StringBuilder();
    if (entity.isShuffle()) flags.append("S");
    if (entity.isSound()) flags.append("\u266a");
    if (entity.isGlowEffect()) flags.append("G");
    if (entity.isParticleTrail()) flags.append("P");
    if (entity.isTweening()) flags.append("T");
    if (entity.isSpeedRun()) flags.append("\u26a1");
    if (entity.isShowStats()) flags.append("\ud83d\udcca");
    if (entity.getSecondAlgorithm() != null) flags.append("VS");
    return flags.toString();
  }
}
