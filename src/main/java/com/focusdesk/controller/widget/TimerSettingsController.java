package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import com.focusdesk.dao.PreferenceDAO;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

/**
 * Controller for the Pomodoro timer settings dialog.
 * Allows customization of focus/break durations and notification preferences.
 */
public class TimerSettingsController {

    @FXML private Slider focusSlider;
    @FXML private Slider breakSlider;
    @FXML private Slider longBreakSlider;
    @FXML private Slider cyclesSlider;
    @FXML private Label focusValueLabel;
    @FXML private Label breakValueLabel;
    @FXML private Label longBreakValueLabel;
    @FXML private Label cyclesValueLabel;
    @FXML private CheckBox soundCheckbox;

    private PreferenceDAO preferenceDAO = new PreferenceDAO();
    private TimerSlideController timerController;

    @FXML
    public void initialize() {
        // Bind slider values to labels for live feedback
        focusSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                focusValueLabel.setText(newVal.intValue() + " min"));
        breakSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                breakValueLabel.setText(newVal.intValue() + " min"));
        longBreakSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                longBreakValueLabel.setText(newVal.intValue() + " min"));
        cyclesSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                cyclesValueLabel.setText(String.valueOf(newVal.intValue())));
    }

    public void setTimerController(TimerSlideController controller) {
        this.timerController = controller;
    }

    @FXML
    private void onSave() {
        try {
            int userId = Session.getCurrentUser().getId();
            int focusMin = (int) focusSlider.getValue();
            int breakMin = (int) breakSlider.getValue();
            int longBreakMin = (int) longBreakSlider.getValue();

            // Update preferences in database
            preferenceDAO.updateTimerPreferences(userId, focusMin, breakMin, longBreakMin);

            // Notify timer controller of changes
            if (timerController != null) {
                // Will implement callback mechanism
            }

            // Close dialog
            closeDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) focusSlider.getScene().getWindow();
        stage.close();
    }
}
