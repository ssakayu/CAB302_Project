package com.focusdesk.controller.widget;

import com.focusdesk.app.App;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/**
 * Manages the floating always-on-top widget window lifecycle and geometry.
 */
public class WidgetWindowManager {

    private Stage ownerStage;
    private Stage widgetStage;

    private final BooleanProperty widgetOpen = new SimpleBooleanProperty(false);

    // Last known widget geometry (in-memory only).
    private double widgetX = Double.NaN;
    private double widgetY = Double.NaN;
    private double widgetWidth = 380;
    private double widgetHeight = 280;

    public void setOwnerStage(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    public void open() {
        if (isOpen()) {
            widgetStage.toFront();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/widget.fxml"));
            Parent root = loader.load();
            Rectangle clip = new Rectangle();
            clip.setArcWidth(36);
            clip.setArcHeight(36);
            clip.widthProperty().bind(Bindings.createDoubleBinding(
                () -> root.getLayoutBounds().getWidth(),
                root.layoutBoundsProperty()
            ));
            clip.heightProperty().bind(Bindings.createDoubleBinding(
                () -> root.getLayoutBounds().getHeight(),
                root.layoutBoundsProperty()
            ));
            root.setClip(clip);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            widgetStage = new Stage();
            widgetStage.initStyle(StageStyle.TRANSPARENT);
            widgetStage.setAlwaysOnTop(true);
            widgetStage.setTitle("FocusDesk Widget");

            widgetStage.setMinWidth(300);
            widgetStage.setMinHeight(240);
            widgetStage.setMaxWidth(500);
            widgetStage.setMaxHeight(360);

            widgetStage.setWidth(widgetWidth);
            widgetStage.setHeight(widgetHeight);

            if (!Double.isNaN(widgetX)) {
                widgetStage.setX(widgetX);
                widgetStage.setY(widgetY);
            }

            widgetStage.xProperty().addListener((obs, o, n) -> widgetX = n.doubleValue());
            widgetStage.yProperty().addListener((obs, o, n) -> widgetY = n.doubleValue());
            widgetStage.widthProperty().addListener((obs, o, n) -> widgetWidth = n.doubleValue());
            widgetStage.heightProperty().addListener((obs, o, n) -> widgetHeight = n.doubleValue());

            widgetStage.setOnHiding(e -> {
                widgetX = widgetStage.getX();
                widgetY = widgetStage.getY();
                widgetWidth = widgetStage.getWidth();
                widgetHeight = widgetStage.getHeight();
                widgetOpen.set(false);
            });

            widgetStage.setOnHidden(e -> widgetStage = null);

            widgetStage.setScene(scene);
            widgetStage.show();
            widgetOpen.set(true);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load widget.fxml", e);
        }
    }

    public void close() {
        if (widgetStage != null) {
            widgetStage.hide();
        }
    }

    public boolean isOpen() {
        return widgetStage != null && widgetStage.isShowing();
    }

    public BooleanProperty openProperty() {
        return widgetOpen;
    }
}