import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects; /*test*/

public class Main extends Application {

    private final List<CheckBox> taskCheckBoxes = new ArrayList<>();
    private final List<CheckBox> widgetCheckBoxes = new ArrayList<>();

    private final List<Region> navSquares = new ArrayList<>();
    private final List<Label> navLabels = new ArrayList<>();

    private Label remainingTasksLabel;
    private Label tasksDoneValueLabel;
    private Label enabledSlidesLabel;

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        root.setTop(createTopBar());
        root.setLeft(createSidebar());
        root.setCenter(createMainContent());

        Scene scene = new Scene(root, 1050, 680);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm()
        );

        stage.setTitle("Focus desk");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(620);
        stage.show();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 16, 8, 16));

        HBox leftGroup = new HBox(18);
        leftGroup.setAlignment(Pos.CENTER_LEFT);

        HBox dots = new HBox(12);
        dots.setAlignment(Pos.CENTER_LEFT);

        dots.getChildren().addAll(
                coloredDot("dot-red"),
                coloredDot("dot-yellow"),
                coloredDot("dot-green")
        );

        Label title = new Label("Focus desk");
        title.getStyleClass().add("window-title");

        leftGroup.getChildren().addAll(dots, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button widgetButton = new Button("●  widget");
        widgetButton.getStyleClass().add("widget-btn");

        Button menuButton = new Button("•••");
        menuButton.getStyleClass().add("menu-btn");

        topBar.getChildren().addAll(leftGroup, spacer, widgetButton, menuButton);

        return topBar;
    }

    private Region coloredDot(String className) {
        Region dot = new Region();
        dot.getStyleClass().addAll("dot", className);
        return dot;
    }

    private VBox createSidebar() {
        navSquares.clear();
        navLabels.clear();

        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(18, 20, 14, 20));
        sidebar.setPrefWidth(250);

        Label overview = sidebarHeading("Overview");

        VBox overviewItems = new VBox(11);
        overviewItems.getChildren().addAll(
                sidebarItem("dashboard", true),
                sidebarItem("Profile", false)
        );

        Label features = sidebarHeading("Features");

        VBox featureItems = new VBox(11);
        featureItems.getChildren().addAll(
                sidebarItem("Notes", false),
                sidebarItem("Tasks", false),
                sidebarItem("Calendar", false),
                sidebarItem("Music", false)
        );

        Label settings = sidebarHeading("settings");

        VBox settingsItems = new VBox(11);
        settingsItems.getChildren().addAll(
                sidebarItem("Widget", false),
                sidebarItem("Appearance", false)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox launchBox = new VBox(6);
        launchBox.getStyleClass().add("launch-box");

        Button launchButton = new Button("● launch widget");
        launchButton.getStyleClass().add("launch-btn");

        Label launchText = new Label("or use button in titlebar");
        launchText.getStyleClass().add("helper-text");

        launchBox.getChildren().addAll(launchButton, launchText);

        sidebar.getChildren().addAll(
                overview,
                overviewItems,
                features,
                featureItems,
                settings,
                settingsItems,
                spacer,
                launchBox
        );

        return sidebar;
    }

    private Label sidebarHeading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sidebar-heading");
        return label;
    }

    private HBox sidebarItem(String text, boolean active) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("nav-row");

        Region square = new Region();
        square.getStyleClass().add("nav-square");

        Label label = new Label(text);
        label.getStyleClass().add("nav-text");

        navSquares.add(square);
        navLabels.add(label);

        row.setOnMouseClicked(event -> setActiveSidebarItem(square, label));

        row.getChildren().addAll(square, label);

        if (active) {
            setActiveSidebarItem(square, label);
        }

        return row;
    }

    private void setActiveSidebarItem(Region selectedSquare, Label selectedLabel) {
        for (Region square : navSquares) {
            square.getStyleClass().remove("nav-square-active");

            if (!square.getStyleClass().contains("nav-square")) {
                square.getStyleClass().add("nav-square");
            }
        }

        for (Label label : navLabels) {
            label.getStyleClass().remove("nav-text-active");
        }

        selectedSquare.getStyleClass().remove("nav-square");
        selectedSquare.getStyleClass().add("nav-square-active");

        selectedLabel.getStyleClass().add("nav-text-active");
    }

    private VBox createMainContent() {
        VBox main = new VBox(10);
        main.getStyleClass().add("main-area");
        main.setPadding(new Insets(14, 14, 14, 14));

        VBox titleBox = new VBox(-4);

        Label title = new Label("Dashboard");
        title.getStyleClass().add("dashboard-title");

        Label subtitle = new Label("Wednesday, week 5");
        subtitle.getStyleClass().add("dashboard-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        HBox statsRow = new HBox(14);
        statsRow.getChildren().addAll(
                createStatCard("Pomodoros today", "1"),
                createStatCard("focus time", "60min"),
                createStatCard("Tasks done", "0/0")
        );

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(52);

        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(48);

        grid.getColumnConstraints().addAll(leftColumn, rightColumn);

        VBox tasksCard = createTasksCard();
        VBox calendarCard = createCalendarCard();
        VBox musicCard = createMusicCard();
        VBox widgetSettingsCard = createWidgetSettingsCard();

        grid.add(tasksCard, 0, 0);
        grid.add(calendarCard, 1, 0);
        grid.add(musicCard, 0, 1);
        grid.add(widgetSettingsCard, 1, 1);

        GridPane.setHgrow(tasksCard, Priority.ALWAYS);
        GridPane.setHgrow(calendarCard, Priority.ALWAYS);
        GridPane.setHgrow(musicCard, Priority.ALWAYS);
        GridPane.setHgrow(widgetSettingsCard, Priority.ALWAYS);

        main.getChildren().addAll(titleBox, statsRow, grid);

        return main;
    }

    private VBox createStatCard(String heading, String value) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setPrefHeight(82);
        card.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(heading);
        title.getStyleClass().add("stat-heading");

        Label number = new Label(value);
        number.getStyleClass().add("stat-value");

        if (heading.equals("Tasks done")) {
            tasksDoneValueLabel = number;
        }

        card.getChildren().addAll(title, number);
        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    private VBox createTasksCard() {
        taskCheckBoxes.clear();

        VBox card = new VBox(9);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(13, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Tasks");
        title.getStyleClass().add("content-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        remainingTasksLabel = new Label();
        remainingTasksLabel.getStyleClass().add("remaining-badge");

        header.getChildren().addAll(title, spacer, remainingTasksLabel);

        VBox taskList = new VBox(4);
        taskList.getChildren().addAll(
                taskRow("Read lecture slides", true, "red-pill"),
                taskRow("CAB302 sprint plan", false, "red-pill"),
                taskRow("Write user stories", true, "red-pill"),
                taskRow("CAB302 UI design", false, "yellow-pill")
        );

        card.getChildren().addAll(header, taskList);

        updateTaskStats();

        return card;
    }

    private HBox taskRow(String text, boolean checked, String defaultPillClass) {
        HBox row = new HBox(10);
        row.getStyleClass().add("task-row");
        row.setAlignment(Pos.CENTER_LEFT);

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(checked);
        checkBox.getStyleClass().add("task-checkbox");

        Label label = new Label(text);
        label.getStyleClass().add("task-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Region pill = new Region();
        pill.getStyleClass().add("status-pill");

        updateSingleTaskStyle(checkBox, label, pill, defaultPillClass);

        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateSingleTaskStyle(checkBox, label, pill, defaultPillClass);
            updateTaskStats();
        });

        taskCheckBoxes.add(checkBox);

        row.getChildren().addAll(checkBox, label, spacer, pill);

        return row;
    }

    private void updateSingleTaskStyle(CheckBox checkBox, Label label, Region pill, String defaultPillClass) {
        pill.getStyleClass().removeAll("green-pill", "red-pill", "yellow-pill");
        label.getStyleClass().remove("task-completed");

        if (checkBox.isSelected()) {
            pill.getStyleClass().add("green-pill");
            label.getStyleClass().add("task-completed");
        } else {
            pill.getStyleClass().add(defaultPillClass);
        }
    }

    private void updateTaskStats() {
        int totalTasks = taskCheckBoxes.size();
        int completedTasks = 0;

        for (CheckBox checkBox : taskCheckBoxes) {
            if (checkBox.isSelected()) {
                completedTasks++;
            }
        }

        int remainingTasks = totalTasks - completedTasks;

        if (tasksDoneValueLabel != null) {
            tasksDoneValueLabel.setText(completedTasks + "/" + totalTasks);
        }

        if (remainingTasksLabel != null) {
            remainingTasksLabel.setText(remainingTasks + " remaining");
        }
    }

    private VBox createCalendarCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(13, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Today’s calendar");
        title.getStyleClass().add("content-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label events = new Label("2 events");
        events.getStyleClass().add("green-badge");

        header.getChildren().addAll(title, spacer, events);

        VBox eventList = new VBox(8);
        eventList.getChildren().addAll(
                calendarEvent("CAB302 tutorial", "10:00-12:00"),
                calendarEvent("Team meeting", "19:00-20:00")
        );

        card.getChildren().addAll(header, eventList);

        return card;
    }

    private VBox calendarEvent(String title, String time) {
        VBox box = new VBox(1);
        box.getStyleClass().add("calendar-event");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("event-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dot = new Label("●");
        dot.getStyleClass().add("event-dot");

        row.getChildren().addAll(titleLabel, spacer, dot);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("event-time");

        box.getChildren().addAll(row, timeLabel);

        return box;
    }

    private VBox createMusicCard() {
        VBox card = new VBox(9);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(13, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Now playing");
        title.getStyleClass().add("content-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label connected = new Label("connected");
        connected.getStyleClass().add("green-badge");

        header.getChildren().addAll(title, spacer, connected);

        VBox player = new VBox(4);
        player.getStyleClass().add("music-player");
        player.setPadding(new Insets(9, 13, 8, 13));
        player.setPrefSize(300, 115);

        Label smallTitle = new Label("Music");
        smallTitle.getStyleClass().add("music-small");

        Region blankSpace = new Region();
        blankSpace.setPrefHeight(35);

        Label song = new Label("Cruel Summer\nTaylor Swift");
        song.getStyleClass().add("song-info");

        HBox progress = new HBox(5);
        progress.setAlignment(Pos.CENTER_LEFT);

        Label start = new Label("1:01");
        start.getStyleClass().add("time-text");

        Region line = new Region();
        line.getStyleClass().add("progress-line");
        HBox.setHgrow(line, Priority.ALWAYS);

        Label end = new Label("3:15");
        end.getStyleClass().add("time-text");

        progress.getChildren().addAll(start, line, end);

        Label controls = new Label("⇄     |◀     ▶     ▶|     ↻");
        controls.getStyleClass().add("music-controls");

        player.getChildren().addAll(smallTitle, blankSpace, song, progress, controls);

        card.getChildren().addAll(header, player);

        return card;
    }

    private VBox createWidgetSettingsCard() {
        widgetCheckBoxes.clear();

        VBox card = new VBox(9);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(13, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("widget settings");
        title.getStyleClass().add("content-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox alwaysOnTop = new CheckBox("always on top");
        alwaysOnTop.setSelected(false);
        alwaysOnTop.getStyleClass().add("always-checkbox");

        alwaysOnTop.selectedProperty().addListener((observable, oldValue, newValue) -> {
            primaryStage.setAlwaysOnTop(newValue);
        });

        header.getChildren().addAll(title, spacer, alwaysOnTop);

        enabledSlidesLabel = new Label();
        enabledSlidesLabel.getStyleClass().add("enabled-slides-label");

        VBox settings = new VBox(8);
        settings.getChildren().addAll(
                settingRow("Music slides", true),
                settingRow("Timer slides", true),
                settingRow("Calendar slides", false),
                settingRow("Notes slides", true)
        );

        card.getChildren().addAll(header, enabledSlidesLabel, settings);

        updateWidgetStats();

        return card;
    }

    private HBox settingRow(String text, boolean enabled) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(text);
        label.getStyleClass().add("setting-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(enabled);
        checkBox.getStyleClass().add("setting-checkbox");

        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateWidgetStats();
        });

        widgetCheckBoxes.add(checkBox);

        row.getChildren().addAll(label, spacer, checkBox);

        return row;
    }

    private void updateWidgetStats() {
        int totalSlides = widgetCheckBoxes.size();
        int enabledSlides = 0;

        for (CheckBox checkBox : widgetCheckBoxes) {
            if (checkBox.isSelected()) {
                enabledSlides++;
            }
        }

        if (enabledSlidesLabel != null) {
            enabledSlidesLabel.setText(enabledSlides + "/" + totalSlides + " widget slides enabled");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}