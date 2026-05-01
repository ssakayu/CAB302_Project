package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import com.focusdesk.dao.PreferenceDAO;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Pomodoro timer settings dialog.
 * Allows customization of focus/break durations and notification preferences.
 */
public class TimerSettingsController {

    @FXML private Spinner<Integer> focusSpinner;
    @FXML private Spinner<Integer> breakSpinner;
    @FXML private Spinner<Integer> longBreakSpinner;
    @FXML private Spinner<Integer> cyclesSpinner;
    @FXML private CheckBox soundCheckbox;

    private PreferenceDAO preferenceDAO = new PreferenceDAO();
    private TimerSlideController timerController;

    @FXML
    public void initialize() {
    focusSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 90, 25));
    breakSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
    longBreakSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 60, 15));
    cyclesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 4));

    focusSpinner.setEditable(true);
    breakSpinner.setEditable(true);
    longBreakSpinner.setEditable(true);
    cyclesSpinner.setEditable(true);
    }

    public void setTimerController(TimerSlideController controller) {
        this.timerController = controller;
    }

    public void loadInitialValues(int focusMin, int shortBreakMin, int longBreakMin, int cycles) {
        focusSpinner.getValueFactory().setValue(focusMin);
        breakSpinner.getValueFactory().setValue(shortBreakMin);
        longBreakSpinner.getValueFactory().setValue(longBreakMin);
        cyclesSpinner.getValueFactory().setValue(cycles);
    }

    @FXML
    private void onSave() {
        try {
            int userId = Session.get().getCurrentUser().getId();
            int focusMin = focusSpinner.getValue();
            int breakMin = breakSpinner.getValue();
            int longBreakMin = longBreakSpinner.getValue();
            int cycles = cyclesSpinner.getValue();

            // Update preferences in database
            preferenceDAO.updateTimerPreferences(userId, focusMin, breakMin, longBreakMin);

            // Notify timer controller of changes
            if (timerController != null) {
                timerController.applyTimerSettings(focusMin, breakMin, longBreakMin, cycles);
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
        Stage stage = (Stage) focusSpinner.getScene().getWindow();
        stage.close();
    }
}
