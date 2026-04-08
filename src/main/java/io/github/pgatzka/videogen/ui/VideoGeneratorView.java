package io.github.pgatzka.videogen.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.github.pgatzka.ApplicationProperties;
import io.github.pgatzka.videogen.algorithm.AlgorithmRegistry;
import io.github.pgatzka.videogen.job.VideoJobEntity;
import io.github.pgatzka.videogen.job.VideoJobRequest;
import io.github.pgatzka.videogen.job.VideoJobService;
import io.github.pgatzka.videogen.job.VideoJobStatus;
import io.github.pgatzka.videogen.visualization.VisualizationRegistry;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Route("")
public class VideoGeneratorView extends VerticalLayout {

  private final VideoJobService jobService;
  private final Grid<VideoJobEntity> jobGrid;
  private UI ui;

  public VideoGeneratorView(
      VideoJobService jobService,
      AlgorithmRegistry algorithmRegistry,
      VisualizationRegistry visualizationRegistry,
      ApplicationProperties properties) {
    this.jobService = jobService;

    setSizeFull();
    setPadding(true);
    setSpacing(true);

    add(new H2(getTranslation("view.title")));

    // Row 1: Core settings
    Select<String> algorithmSelect = new Select<>();
    algorithmSelect.setLabel(getTranslation("form.algorithm"));
    algorithmSelect.setItems(algorithmRegistry.getAllNames());
    algorithmSelect.setValue(algorithmRegistry.getAllNames().getFirst());

    Select<String> secondAlgorithmSelect = new Select<>();
    secondAlgorithmSelect.setLabel(getTranslation("form.second-algorithm"));
    secondAlgorithmSelect.setItems(algorithmRegistry.getAllNames());
    secondAlgorithmSelect.setEmptySelectionAllowed(true);
    secondAlgorithmSelect.setEmptySelectionCaption(getTranslation("form.none"));

    Select<String> visualizationSelect = new Select<>();
    visualizationSelect.setLabel(getTranslation("form.visualization"));
    visualizationSelect.setItems(visualizationRegistry.getAllNames());
    visualizationSelect.setValue(visualizationRegistry.getAllNames().getFirst());

    IntegerField elementCountField = new IntegerField(getTranslation("form.elements"));
    elementCountField.setValue(properties.getDefaultElementCount());
    elementCountField.setMin(2);
    elementCountField.setMax(200);
    elementCountField.setStepButtonsVisible(true);

    IntegerField fpsField = new IntegerField(getTranslation("form.fps"));
    fpsField.setValue(properties.getDefaultFps());
    fpsField.setMin(24);
    fpsField.setMax(120);
    fpsField.setStepButtonsVisible(true);

    IntegerField framesPerStepField = new IntegerField(getTranslation("form.frames-per-step"));
    framesPerStepField.setValue(properties.getDefaultFramesPerStep());
    framesPerStepField.setMin(1);
    framesPerStepField.setMax(10);
    framesPerStepField.setStepButtonsVisible(true);

    Select<String> colorSchemeSelect = new Select<>();
    colorSchemeSelect.setLabel(getTranslation("form.color-scheme"));
    colorSchemeSelect.setItems("DEFAULT", "RAINBOW", "GREYSCALE");
    colorSchemeSelect.setValue(properties.getDefaultColorScheme());

    HorizontalLayout row1 =
        new HorizontalLayout(
            algorithmSelect,
            secondAlgorithmSelect,
            visualizationSelect,
            elementCountField,
            fpsField,
            framesPerStepField,
            colorSchemeSelect);
    row1.setAlignItems(Alignment.END);
    row1.setWidthFull();

    // Row 2: Toggles
    Checkbox shuffleCheckbox = new Checkbox(getTranslation("form.shuffle"));
    shuffleCheckbox.setValue(properties.isDefaultShuffle());

    Checkbox soundCheckbox = new Checkbox(getTranslation("form.sound"));
    soundCheckbox.setValue(properties.isDefaultSound());

    Checkbox statsCheckbox = new Checkbox(getTranslation("form.show-stats"));
    statsCheckbox.setValue(properties.isDefaultShowStats());

    Checkbox glowCheckbox = new Checkbox(getTranslation("form.glow-effect"));
    glowCheckbox.setValue(properties.isDefaultGlowEffect());

    Checkbox particleCheckbox = new Checkbox(getTranslation("form.particle-trail"));
    particleCheckbox.setValue(properties.isDefaultParticleTrail());

    Checkbox tweeningCheckbox = new Checkbox(getTranslation("form.tweening"));
    tweeningCheckbox.setValue(properties.isDefaultTweening());

    Checkbox speedRunCheckbox = new Checkbox(getTranslation("form.speed-run"));
    speedRunCheckbox.setValue(properties.isDefaultSpeedRun());

    Checkbox debugCheckbox = new Checkbox(getTranslation("form.debug"));
    debugCheckbox.setValue(false);

    Button generateButton = new Button(getTranslation("form.generate"));
    generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateButton.addClickListener(
        event -> {
          log.info("Generate button clicked");
          VideoJobRequest request =
              VideoJobRequest.builder()
                  .algorithm(algorithmSelect.getValue())
                  .visualization(visualizationSelect.getValue())
                  .elementCount(elementCountField.getValue())
                  .fps(fpsField.getValue())
                  .width(properties.getDefaultWidth())
                  .height(properties.getDefaultHeight())
                  .framesPerStep(framesPerStepField.getValue())
                  .shuffle(shuffleCheckbox.getValue())
                  .colorScheme(colorSchemeSelect.getValue())
                  .sound(soundCheckbox.getValue())
                  .showStats(statsCheckbox.getValue())
                  .glowEffect(glowCheckbox.getValue())
                  .particleTrail(particleCheckbox.getValue())
                  .tweening(tweeningCheckbox.getValue())
                  .speedRun(speedRunCheckbox.getValue())
                  .secondAlgorithm(secondAlgorithmSelect.getValue())
                  .debug(debugCheckbox.getValue())
                  .build();
          jobService.submitJob(request);
          Notification.show(getTranslation("form.job-submitted"));
          refreshGrid();
        });

    HorizontalLayout row2 =
        new HorizontalLayout(
            shuffleCheckbox,
            soundCheckbox,
            statsCheckbox,
            glowCheckbox,
            particleCheckbox,
            tweeningCheckbox,
            speedRunCheckbox,
            debugCheckbox,
            generateButton);
    row2.setAlignItems(Alignment.END);
    row2.setWidthFull();

    add(row1, row2);

    // Jobs grid
    jobGrid = new Grid<>(VideoJobEntity.class, false);
    jobGrid
        .addColumn(entity -> entity.getId().toString().substring(0, 8))
        .setHeader(getTranslation("grid.column.id"));
    jobGrid
        .addColumn(VideoJobEntity::getAlgorithm)
        .setHeader(getTranslation("grid.column.algorithm"));
    jobGrid
        .addColumn(VideoJobEntity::getVisualization)
        .setHeader(getTranslation("grid.column.visualization"));
    jobGrid
        .addColumn(VideoJobEntity::getElementCount)
        .setHeader(getTranslation("grid.column.elements"));

    jobGrid
        .addComponentColumn(
            entity -> {
              Span statusBadge = new Span(entity.getStatus().name());
              String theme =
                  switch (entity.getStatus()) {
                    case QUEUED -> "badge";
                    case RUNNING -> "badge primary";
                    case COMPLETED -> "badge success";
                    case FAILED -> "badge error";
                  };
              statusBadge.getElement().setAttribute("theme", theme);
              return statusBadge;
            })
        .setHeader(getTranslation("grid.column.status"));

    jobGrid
        .addComponentColumn(
            entity -> {
              ProgressBar progressBar = new ProgressBar();
              progressBar.setMin(0);
              progressBar.setMax(100);
              progressBar.setValue(entity.getProgress());
              Span percentLabel = new Span(entity.getProgress() + "%");
              percentLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
              HorizontalLayout layout = new HorizontalLayout(progressBar, percentLabel);
              layout.setAlignItems(Alignment.CENTER);
              layout.setFlexGrow(1, progressBar);
              layout.setWidthFull();
              return layout;
            })
        .setHeader(getTranslation("grid.column.progress"));

    jobGrid
        .addColumn(entity -> entity.getStatusMessage() != null ? entity.getStatusMessage() : "")
        .setHeader(getTranslation("grid.column.current-step"));

    jobGrid
        .addComponentColumn(
            entity -> {
              if (entity.getStatus() == VideoJobStatus.COMPLETED
                  && entity.getOutputPath() != null
                  && Files.exists(Path.of(entity.getOutputPath()))) {
                String filename = Path.of(entity.getOutputPath()).getFileName().toString();
                StreamResource resource =
                    new StreamResource(
                        filename,
                        () -> {
                          try {
                            return new FileInputStream(entity.getOutputPath());
                          } catch (FileNotFoundException e) {
                            log.error("Video file not found: {}", entity.getOutputPath(), e);
                            return null;
                          }
                        });
                resource.setContentType("video/mp4");
                Anchor download = new Anchor(resource, getTranslation("grid.action.download"));
                download.getElement().setAttribute("download", true);
                return download;
              } else if (entity.getStatus() == VideoJobStatus.FAILED) {
                Button detailsBtn = new Button(getTranslation("grid.action.details"));
                detailsBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                detailsBtn.addClickListener(
                    e ->
                        Notification.show(
                            entity.getErrorMessage() != null
                                ? entity.getErrorMessage()
                                : getTranslation("grid.error.unknown"),
                            5000,
                            Notification.Position.MIDDLE));
                return detailsBtn;
              }
              return new Span();
            })
        .setHeader(getTranslation("grid.column.actions"));

    jobGrid.setSizeFull();
    add(jobGrid);
    setFlexGrow(1, jobGrid);

    refreshGrid();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    this.ui = attachEvent.getUI();
    ui.setPollInterval(2000);
    ui.addPollListener(event -> refreshGrid());
    log.info("VideoGeneratorView attached, polling enabled (2s interval)");
  }

  private void refreshGrid() {
    jobGrid.setItems(jobService.getAllJobs());
  }
}
