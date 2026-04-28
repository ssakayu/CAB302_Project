package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class WidgetController {

    @FXML private HBox header;
    @FXML private Label slideTitle;
    @FXML private StackPane slideContainer;
    @FXML private HBox dotsContainer;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private static final String[] TITLES = {"To-Do", "Timer", "Notes", "Calendar", "Music"};

    private List<Node> slides;
    private int currentIndex = 0;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    public void initialize() {
        // Snapshot the slide list once — StackPane children order is the slide order
        slides = List.copyOf(slideContainer.getChildren());

        // Only the first slide is visible; rest are transparent and non-interactive
        for (int i = 0; i < slides.size(); i++) {
            slides.get(i).setOpacity(i == 0 ? 1.0 : 0.0);
            slides.get(i).setMouseTransparent(i != 0);
        }

        buildDots();
        syncHeader();
        setupDrag();
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    @FXML
    private void onPrev() {
        if (currentIndex > 0) showSlide(currentIndex - 1);
    }

    @FXML
    private void onNext() {
        if (currentIndex < slides.size() - 1) showSlide(currentIndex + 1);
    }

    private void showSlide(int newIndex) {
        Node from = slides.get(currentIndex);
        Node to   = slides.get(newIndex);
        currentIndex = newIndex;

        // Fade out the departing slide
        FadeTransition out = new FadeTransition(Duration.millis(120), from);
        out.setToValue(0.0);
        out.setOnFinished(e -> from.setMouseTransparent(true));
        out.play();

        // Fade in the arriving slide simultaneously
        to.setMouseTransparent(false);
        FadeTransition in = new FadeTransition(Duration.millis(120), to);
        in.setFromValue(0.0);
        in.setToValue(1.0);
        in.play();

        buildDots();
        syncHeader();
    }

    // -------------------------------------------------------------------------
    // Dot indicators
    // -------------------------------------------------------------------------

    private void buildDots() {
        dotsContainer.getChildren().clear();
        for (int i = 0; i < slides.size(); i++) {
            Region dot = new Region();
            dot.getStyleClass().add("dot");
            if (i == currentIndex) dot.getStyleClass().add("dot-active");
            dotsContainer.getChildren().add(dot);
        }
    }

    private void syncHeader() {
        slideTitle.setText(TITLES[currentIndex]);
        prevBtn.setDisable(currentIndex == 0);
        nextBtn.setDisable(currentIndex == slides.size() - 1);
    }

    // -------------------------------------------------------------------------
    // Drag — move the window by dragging the header
    // -------------------------------------------------------------------------

    private void setupDrag() {
        header.setOnMousePressed(e -> {
            Stage stage = (Stage) header.getScene().getWindow();
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });

        header.setOnMouseDragged(e -> {
            Stage stage = (Stage) header.getScene().getWindow();
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }

    // -------------------------------------------------------------------------
    // Close
    // -------------------------------------------------------------------------

    @FXML
    private void onClose() {
        Session.get().closeWidget();
    }
}
