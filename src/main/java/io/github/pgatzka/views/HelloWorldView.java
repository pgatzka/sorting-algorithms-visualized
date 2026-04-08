package io.github.pgatzka.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class HelloWorldView extends VerticalLayout {

  public HelloWorldView() {
    Span greeting = new Span("Hello, World!");
    Button button =
        new Button("Click me", event -> greeting.setText("Hello from Vaadin 25 + Spring Boot 4!"));

    add(greeting, button);
  }
}
