package io.github.pgatzka.videogen.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("privacy")
public class PrivacyPolicyView extends VerticalLayout {

  public PrivacyPolicyView() {
    setPadding(true);
    setSpacing(true);
    setMaxWidth("800px");
    getStyle().set("margin", "0 auto");

    add(new H2("Privacy Policy"));
    add(new Paragraph("Last updated: 2026-04-08"));

    add(new H3("1. Overview"));
    add(
        new Paragraph(
            "Sorting Algorithms Visualized is a self-hosted application that runs "
                + "locally on a private network. It generates sorting algorithm visualization "
                + "videos and optionally uploads them to TikTok."));

    add(new H3("2. Data Collected"));
    add(
        new Paragraph(
            "This application only stores: TikTok OAuth tokens for API access, "
                + "video generation job metadata, and TikTok video performance metrics "
                + "(views, likes, comments, shares). No personal user data is collected."));

    add(new H3("3. Third-Party Services"));
    add(
        new Paragraph(
            "This application connects to the TikTok API solely for uploading videos "
                + "and retrieving performance metrics. No data is shared with any other "
                + "third party."));

    add(new H3("4. Data Storage"));
    add(
        new Paragraph(
            "All data is stored locally on the machine running this application. "
                + "No data is transmitted externally except to the TikTok API."));

    add(new H3("5. Contact"));
    add(new Paragraph("For questions, contact the application administrator."));
  }
}
