package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
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
    @FXML private Region resizeHandle;

    private static final String[] TITLES = {"To-Do", "Timer", "Notes", "Calendar", "Music"};
    private static final double MIN_W = 300, MAX_W = 500;
    private static final double MIN_H = 150, MAX_H = 300;

    private List<Node> slides;
    private int currentIndex = 0;

    // Drag state
    private double dragOffsetX, dragOffsetY;

    // Resize state
    private double resizeStartX, resizeStartY;
    private double resizeStartW, resizeStartH;

    @FXML
    public void initialize() {
        slides = List.copyOf(slideContainer.getChildren());

        for (int i = 0; i < slides.size(); i++) {
            slides.get(i).setOpacity(i == 0 ? 1.0 : 0.0);
            slides.get(i).setMouseTransparent(i != 0);
        }

        buildDots();
        syncHeader();
        setupDrag();
        setupResize();
    }

    // -------------------------------------------------------------------------
    // Navigation — wraps around at both ends
    // -------------------------------------------------------------------------

    @FXML
    private void onPrev() {
        showSlide((currentIndex - 1 + slides.size()) % slides.size());
    }

    @FXML
    private void onNext() {
        showSlide((currentIndex + 1) % slides.size());
    }

    private void showSlide(int newIndex) {
        Node from = slides.get(currentIndex);
        Node to   = slides.get(newIndex);
        currentIndex = newIndex;

        FadeTransition out = new FadeTransition(Duration.millis(120), from);
        out.setToValue(0.0);
        out.setOnFinished(e -> from.setMouseTransparent(true));
        out.play();

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
    // Resize — SE corner handle
    // -------------------------------------------------------------------------

    private void setupResize() {
        resizeHandle.setCursor(Cursor.SE_RESIZE);

        resizeHandle.setOnMousePressed(e -> {
            Stage stage = (Stage) resizeHandle.getScene().getWindow();
            resizeStartX = e.getScreenX();
            resizeStartY = e.getScreenY();
            resizeStartW = stage.getWidth();
            resizeStartH = stage.getHeight();
            e.consume();
        });

        resizeHandle.setOnMouseDragged(e -> {
            Stage stage = (Stage) resizeHandle.getScene().getWindow();
            double newW = resizeStartW + (e.getScreenX() - resizeStartX);
            double newH = resizeStartH + (e.getScreenY() - resizeStartY);
            stage.setWidth(Math.max(MIN_W, Math.min(MAX_W, newW)));
            stage.setHeight(Math.max(MIN_H, Math.min(MAX_H, newH)));
            e.consume();
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
