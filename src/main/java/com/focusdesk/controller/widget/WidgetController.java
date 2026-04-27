package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;

public class WidgetController {

    @FXML private HBox headerBar;
    @FXML private Label slideTitleLabel;
    @FXML private Label slideCounterLabel;
    @FXML private StackPane slideHost;

    private double dragOffsetX;
    private double dragOffsetY;

    private int currentIndex = 0;

    private final List<SlideDef> slides = List.of(
            new SlideDef("Pomodoro", "/fxml/timer_slide.fxml"),
            new SlideDef("Calendar", "/fxml/calendar_slide.fxml"),
            new SlideDef("Music", "/fxml/music_slide.fxml"),
            new SlideDef("To-Do", "/fxml/todo_slide.fxml")
    );

    @FXML
    public void initialize() {
        showSlide(0);
    }

    @FXML
    private void showPreviousSlide() {
        int next = (currentIndex - 1 + slides.size()) % slides.size();
        showSlide(next);
    }

    @FXML
    private void showNextSlide() {
        int next = (currentIndex + 1) % slides.size();
        showSlide(next);
    }

    @FXML
    private void startDrag(MouseEvent event) {
        Stage stage = getStage();
        if (stage == null) return;

        dragOffsetX = event.getSceneX();
        dragOffsetY = event.getSceneY();
    }

    @FXML
    private void dragWidget(MouseEvent event) {
        Stage stage = getStage();
        if (stage == null) return;

        stage.setX(event.getScreenX() - dragOffsetX);
        stage.setY(event.getScreenY() - dragOffsetY);
    }

    @FXML
    private void minimizeToMain() {
        Stage widgetStage = getStage();
        if (widgetStage != null) {
            widgetStage.hide();
        }

        Stage mainStage = Session.get().getMainStage();
        if (mainStage != null) {
            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
            mainStage.requestFocus();
        }
    }

    private Stage getStage() {
        if (headerBar == null || headerBar.getScene() == null || headerBar.getScene().getWindow() == null) {
            return null;
        }
        return (Stage) headerBar.getScene().getWindow();
    }

    private void showSlide(int index) {
        currentIndex = index;
        SlideDef def = slides.get(index);
        if (slideTitleLabel != null) {
            slideTitleLabel.setText(def.title());
        }
        if (slideCounterLabel != null) {
            slideCounterLabel.setText((index + 1) + " / " + slides.size());
        }

        try {
            if (slideHost == null) {
                throw new IllegalStateException("slideHost was not injected from widget.fxml");
            }

            slideHost.getChildren().clear();
            slideHost.getChildren().add(FXMLLoader.load(getClass().getResource(def.fxmlPath())));
        } catch (Exception e) {
            e.printStackTrace();
            slideHost.getChildren().clear();
            slideHost.getChildren().add(new Label("Unable to load " + def.title()));
        }
    }

    private record SlideDef(String title, String fxmlPath) {}
}
