package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import com.focusdesk.model.Task;
import com.focusdesk.model.User;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label userIdLabel;

    @FXML private Label totalTasksLabel;

    @FXML private Label doneTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label notesCountLabel;

    @FXML
    private void initialize() {
        User user = Session.get().getCurrentUser();
        if (user == null)
        {
            usernameLabel.setText("Not signed in");
            emailLabel.setText("—");
            userIdLabel.setText("—");
            totalTasksLabel.setText("0");
            doneTasksLabel.setText("0");
            pendingTasksLabel.setText("0");
            notesCountLabel.setText("0");
            return;
        }

        usernameLabel.setText(user.getUsername());
        emailLabel.setText(user.getEmail());
        userIdLabel.setText("#" + user.getId());

        ObservableList<Task> tasks = Session.get().getTasks();
        int total = tasks == null ? 0 : tasks.size();
        long done = tasks == null ? 0 : tasks.stream().filter(Task::isDone).count();
        long pending = total - done;

        totalTasksLabel.setText(String.valueOf(total));
        doneTasksLabel.setText(String.valueOf(done));
        pendingTasksLabel.setText(String.valueOf(pending));
        notesCountLabel.setText(
                String.valueOf(Session.get().getNotes() == null ? 0 : Session.get().getNotes().size()));
    }

    @FXML
    private void onLogout() {
        Session.get().logout();
        try {
            App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
