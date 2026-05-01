package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import com.focusdesk.dao.TaskDAO;
import com.focusdesk.model.Task;
import com.focusdesk.util.TaskRunner;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class TodoSlideController {

    @FXML private ListView<Task> miniTaskList;
    @FXML private Label emptyHint;

    private static final int MAX_VISIBLE = 6;

    private final TaskDAO dao = new TaskDAO();

    @FXML
    private void initialize() {
        ObservableList<Task> sessionTasks = Session.get().getTasks();
        if (sessionTasks == null) {
            emptyHint.setVisible(true);
            emptyHint.setManaged(true);
            return;
        }

        FilteredList<Task> incomplete = new FilteredList<>(sessionTasks, t -> !t.isDone());
        miniTaskList.setItems(incomplete);
        miniTaskList.setCellFactory(lv -> new MiniTaskCell());

        emptyHint.visibleProperty().bind(
                javafx.beans.binding.Bindings.size(incomplete).isEqualTo(0));
        emptyHint.managedProperty().bind(emptyHint.visibleProperty());
    }

    private void onToggleDone(Task task) {
        boolean newDone = !task.isDone();
        TaskRunner.run(
                () -> { dao.setDone(task.getId(), newDone); return null; },
                ignored -> {
                    ObservableList<Task> all = Session.get().getTasks();
                    if (all == null) return;
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).getId() == task.getId()) {
                            all.set(i, new Task(
                                    task.getId(), task.getUserId(),
                                    task.getTitle(), newDone, task.getPriority()));
                            break;
                        }
                    }
                },
                err -> System.err.println("Widget toggle done failed: " + err.getMessage()));
    }

    private class MiniTaskCell extends ListCell<Task> {
        private final CheckBox checkBox = new CheckBox();
        private final SimpleBooleanProperty hideRow = new SimpleBooleanProperty(false);

        MiniTaskCell() {
            checkBox.getStyleClass().add("widget-todo-check");
            checkBox.setOnAction(e -> {
                if (getItem() != null) onToggleDone(getItem());
            });
            visibleProperty().bind(hideRow.not());
            managedProperty().bind(hideRow.not());
            setText(null);
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            hideRow.set(getIndex() >= MAX_VISIBLE);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }
            checkBox.setSelected(task.isDone());
            checkBox.setText(task.getTitle());
            setGraphic(checkBox);
        }
    }
}
