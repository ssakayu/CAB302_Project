package com.focusdesk.controller;

import com.focusdesk.app.Session;
import com.focusdesk.dao.PreferenceDAO;
import com.focusdesk.dao.TaskDAO;
import com.focusdesk.model.Task;
import com.focusdesk.util.TaskRunner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class TasksController {

    @FXML
    private ChoiceBox<String> filterPriority;
    @FXML
    private ChoiceBox<String> filterStatus;
    @FXML
    private TextField newTaskField;
    @FXML
    private ChoiceBox<String> newTaskPriority;
    @FXML
    private ListView<Task> taskList;

    private final TaskDAO dao = new TaskDAO();
    private final ObservableList<Task> displayList = FXCollections.observableArrayList();
    private boolean initialising = true;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @FXML
    private void initialize() {
        filterPriority.getItems().addAll("All Priorities", "Low", "Medium", "High");
        filterPriority.setValue("All Priorities");

        filterStatus.getItems().addAll("All Tasks", "Incomplete", "Done");
        filterStatus.setValue("All Tasks");

        newTaskPriority.getItems().addAll("Low", "Medium", "High");
        newTaskPriority.setValue("Medium");

        taskList.setItems(displayList);
        taskList.setCellFactory(lv -> new TaskCell());
        taskList.setPlaceholder(new Label("No tasks yet. Add one above!"));
        taskList.getPlaceholder().getStyleClass().add("tasks-empty-label");

        filterPriority.setOnAction(e -> {
            applyFilter();
            if (!initialising)
                saveFilterPreference();
        });
        filterStatus.setOnAction(e -> {
            applyFilter();
            if (!initialising)
                saveFilterPreference();
        });

        loadFilterPreference();
        loadTasks();
        initialising = false;
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private void loadTasks() {
        int userId = Session.get().getCurrentUser().getId();
        TaskRunner.run(
                () -> dao.listByUser(userId),
                tasks -> {
                    Session.get().getTasks().setAll(tasks);
                    applyFilter();
                },
                err -> System.err.println("Load tasks failed: " + err.getMessage()));
    }

    private void applyFilter() {
        ObservableList<Task> all = Session.get().getTasks();
        if (all == null)
            return;

        String priority = filterPriority.getValue();
        String status = filterStatus.getValue();

        displayList.setAll(
                all.stream()
                        .filter(t -> priority == null || priority.equals("All Priorities")
                                || t.getPriority().equalsIgnoreCase(priority))
                        .filter(t -> {
                            if ("Incomplete".equals(status))
                                return !t.isDone();
                            if ("Done".equals(status))
                                return t.isDone();
                            return true;
                        })
                        .toList());
    }

    // -------------------------------------------------------------------------
    // Add task
    // -------------------------------------------------------------------------

    @FXML
    private void onAddTask() {
        String title = newTaskField.getText().trim();
        if (title.isEmpty())
            return;

        String priority = newTaskPriority.getValue().toLowerCase();
        int userId = Session.get().getCurrentUser().getId();

        TaskRunner.run(
                () -> dao.create(userId, title, priority),
                task -> {
                    Session.get().getTasks().add(0, task);
                    newTaskField.clear();
                    applyFilter();
                },
                err -> System.err.println("Add task failed: " + err.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Cell callbacks (called from inner TaskCell)
    // -------------------------------------------------------------------------

    private void onToggleDone(Task task) {
        boolean newDone = !task.isDone();
        TaskRunner.run(
                () -> {
                    dao.setDone(task.getId(), newDone);
                    return null;
                },
                ignored -> {
                    replaceInSession(task, new Task(
                            task.getId(), task.getUserId(), task.getTitle(),
                            newDone, task.getPriority()));
                    applyFilter();
                },
                err -> System.err.println("Toggle done failed: " + err.getMessage()));
    }

    private void onEditTask(Task task, String newTitle, String newPriority) {
        TaskRunner.run(
                () -> {
                    if (!task.getTitle().equals(newTitle))
                        dao.update(task.getId(), newTitle);
                    if (!task.getPriority().equals(newPriority))
                        dao.setPriority(task.getId(), newPriority);
                    return null;
                },
                ignored -> {
                    replaceInSession(task, new Task(
                            task.getId(), task.getUserId(), newTitle,
                            task.isDone(), newPriority));
                    applyFilter();
                },
                err -> System.err.println("Edit task failed: " + err.getMessage()));
    }

    private void onDeleteTask(Task task) {
        TaskRunner.run(
                () -> {
                    dao.delete(task.getId());
                    return null;
                },
                ignored -> {
                    Session.get().getTasks().removeIf(t -> t.getId() == task.getId());
                    applyFilter();
                },
                err -> System.err.println("Delete task failed: " + err.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Filter preference persistence
    // -------------------------------------------------------------------------

    private void loadFilterPreference() {
        int userId = Session.get().getCurrentUser().getId();
        TaskRunner.run(
                () -> new PreferenceDAO().getTaskFilter(userId),
                filter -> {
                    if (filter == null)
                        return;
                    String[] parts = filter.split(":", 2);
                    if (parts.length == 2) {
                        filterPriority.setValue(parts[0]);
                        filterStatus.setValue(parts[1]);
                        applyFilter();
                    }
                },
                err -> {
                } // column may not exist on very old DBs — safe to ignore
        );
    }

    private void saveFilterPreference() {
        int userId = Session.get().getCurrentUser().getId();
        String combined = filterPriority.getValue() + ":" + filterStatus.getValue();
        TaskRunner.run(
                () -> {
                    new PreferenceDAO().saveTaskFilter(userId, combined);
                    return null;
                },
                ignored -> {
                },
                err -> System.err.println("Save filter preference failed: " + err.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void replaceInSession(Task old, Task updated) {
        ObservableList<Task> all = Session.get().getTasks();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId() == old.getId()) {
                all.set(i, updated);
                return;
            }
        }
    }

    // =========================================================================
    // Custom list cell
    // =========================================================================

    private class TaskCell extends ListCell<Task> {

        // Shared in both modes
        private final CheckBox checkBox = new CheckBox();

        // View-mode nodes
        private final Label titleLabel = new Label();
        private final Label priorityBadge = new Label();
        private final Button editBtn = new Button("Edit");
        private final Button deleteBtn = new Button("Delete");

        // Edit-mode nodes
        private final TextField titleField = new TextField();
        private final ChoiceBox<String> priorityChoice = new ChoiceBox<>();
        private final Button saveBtn = new Button("Save");
        private final Button cancelBtn = new Button("Cancel");

        private final HBox row;
        private boolean editing = false;

        TaskCell() {
            priorityChoice.getItems().addAll("low", "medium", "high");

            // Allow title to grow
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            HBox.setHgrow(titleField, Priority.ALWAYS);
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            // Edit-mode nodes hidden by default
            titleField.setVisible(false);
            titleField.setManaged(false);
            priorityChoice.setVisible(false);
            priorityChoice.setManaged(false);
            saveBtn.setVisible(false);
            saveBtn.setManaged(false);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);

            row = new HBox(10,
                    checkBox,
                    titleLabel, titleField,
                    priorityBadge, priorityChoice,
                    editBtn, saveBtn, cancelBtn,
                    deleteBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("task-row");
            row.setPadding(new Insets(10, 14, 10, 14));

            // Style classes
            titleLabel.getStyleClass().add("task-title");
            priorityBadge.getStyleClass().add("priority-badge");
            editBtn.getStyleClass().add("task-action-btn");
            saveBtn.getStyleClass().add("task-action-btn");
            cancelBtn.getStyleClass().add("task-action-btn");
            deleteBtn.getStyleClass().addAll("task-action-btn", "task-delete-btn");
            titleField.getStyleClass().add("task-inline-field");

            // Wire events
            checkBox.setOnAction(e -> {
                if (getItem() != null)
                    onToggleDone(getItem());
            });
            editBtn.setOnAction(e -> startEditing());
            cancelBtn.setOnAction(e -> cancelEditing());
            saveBtn.setOnAction(e -> commitSave());
            deleteBtn.setOnAction(e -> {
                if (getItem() != null)
                    onDeleteTask(getItem());
            });

            // Enter key saves while in edit mode
            titleField.setOnAction(e -> commitSave());

            setText(null);
        }

        // ---- mode switching -------------------------------------------------

        private void startEditing() {
            Task task = getItem();
            if (task == null)
                return;
            titleField.setText(task.getTitle());
            priorityChoice.setValue(task.getPriority());

            titleLabel.setVisible(false);
            titleLabel.setManaged(false);
            priorityBadge.setVisible(false);
            priorityBadge.setManaged(false);
            editBtn.setVisible(false);
            editBtn.setManaged(false);

            titleField.setVisible(true);
            titleField.setManaged(true);
            priorityChoice.setVisible(true);
            priorityChoice.setManaged(true);
            saveBtn.setVisible(true);
            saveBtn.setManaged(true);
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);

            editing = true;
            titleField.requestFocus();
            titleField.selectAll();
        }

        private void cancelEditing() {
            titleField.setVisible(false);
            titleField.setManaged(false);
            priorityChoice.setVisible(false);
            priorityChoice.setManaged(false);
            saveBtn.setVisible(false);
            saveBtn.setManaged(false);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);

            titleLabel.setVisible(true);
            titleLabel.setManaged(true);
            priorityBadge.setVisible(true);
            priorityBadge.setManaged(true);
            editBtn.setVisible(true);
            editBtn.setManaged(true);

            editing = false;
        }

        private void commitSave() {
            Task task = getItem();
            if (task == null)
                return;
            String newTitle = titleField.getText().trim();
            String newPriority = priorityChoice.getValue();
            if (!newTitle.isEmpty())
                onEditTask(task, newTitle, newPriority);
            cancelEditing();
        }

        // ---- cell update ----------------------------------------------------

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }

            if (editing)
                cancelEditing();

            // Checkbox
            checkBox.setSelected(task.isDone());

            // Title
            titleLabel.setText(task.getTitle());
            titleLabel.setStyle(task.isDone()
                    ? "-fx-strikethrough: true; -fx-text-fill: #aaa;"
                    : "-fx-strikethrough: false; -fx-text-fill: #1a1a1a;");

            // Priority badge
            String p = task.getPriority() != null ? task.getPriority() : "medium";
            priorityBadge.setText(p.substring(0, 1).toUpperCase() + p.substring(1));
            priorityBadge.getStyleClass().removeAll("badge-low", "badge-medium", "badge-high");
            priorityBadge.getStyleClass().add("badge-" + p);

            setGraphic(row);
        }
    }
}
