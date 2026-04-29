package com.focusdesk.controller.widget;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarSlideController {

    private static final int START_HOUR = 7;
    private static final int END_HOUR = 22;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter WEEK_RANGE_FMT = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter HEADER_DAY_FMT = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter HEADER_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    @FXML private Label currentWeekLabel;
    @FXML private HBox weekdayHeader;
    @FXML private Label dayPanelTitleLabel;
    @FXML private Label agendaTitleLabel;
    @FXML private Label todayDateLabel;
    @FXML private Label todayWeekLabel;
    @FXML private Label weeklyDayCaptionLabel;
    @FXML private Label eventFeedbackLabel;
    @FXML private Label monthTitleLabel;
    @FXML private Label googleCalendarStatusLabel;
    @FXML private Button googleCalendarButton;
    @FXML private TextField eventTitleField;
    @FXML private TextField eventStartField;
    @FXML private TextField eventEndField;
    @FXML private VBox weeklyDayScheduleBox;
    @FXML private GridPane monthHeaderGrid;
    @FXML private GridPane monthGrid;

    private final List<VBox> dayCards = new ArrayList<>();
    private final Map<LocalDate, List<ScheduleEvent>> customEvents = new HashMap<>();
    private final Set<String> hiddenEventIds = new HashSet<>();

    private LocalDate today;
    private LocalDate weekStart;
    private LocalDate selectedDate;
    private List<ScheduleEvent> events = List.of();

    @FXML
    public void initialize() {
        today = LocalDate.now();
        buildMonthHeader();
        refreshGoogleConnectionUi();
        showWeek(today.with(DayOfWeek.MONDAY), today.getDayOfWeek());
    }

    @FXML
    private void onPreviousWeek() {
        showWeek(weekStart.minusWeeks(1), selectedDate.getDayOfWeek());
    }

    @FXML
    private void onNextWeek() {
        showWeek(weekStart.plusWeeks(1), selectedDate.getDayOfWeek());
    }

    @FXML
    private void onConnectGoogleCalendar() {
        try {
            Desktop.getDesktop().browse(URI.create("https://calendar.google.com/"));
            googleCalendarStatusLabel.setText("Opened Google Calendar in your browser");
            googleCalendarStatusLabel.getStyleClass().removeAll("calendar-connect-status-ok", "calendar-connect-status-off");
            googleCalendarStatusLabel.getStyleClass().add("calendar-connect-status-ok");
        } catch (Exception error) {
            showError("Could not open Google Calendar: " + error.getMessage());
        }
    }

    @FXML
    private void onAddDayEvent() {
        if (selectedDate == null) {
            showEventFeedback("Select a day first.", true);
            return;
        }

        String title = text(eventTitleField);
        String startRaw = text(eventStartField);
        String endRaw = text(eventEndField);
        if (title.isEmpty() || startRaw.isEmpty() || endRaw.isEmpty()) {
            showEventFeedback("Fill in title, start, and end time.", true);
            return;
        }

        Double startHour = parseTime(startRaw);
        Double endHour = parseTime(endRaw);
        if (startHour == null || endHour == null || endHour <= startHour) {
            showEventFeedback("Use a valid time range, e.g. 14:00 to 15:30.", true);
            return;
        }
        if (startHour < START_HOUR || endHour > END_HOUR + 1) {
            showEventFeedback("Events must stay between 7:00 AM and 11:00 PM.", true);
            return;
        }

        int dayIndex = (int) ChronoUnit.DAYS.between(weekStart, selectedDate);
        String eventId = "custom-" + selectedDate + "-" + System.nanoTime();
        ScheduleEvent newEvent = new ScheduleEvent(
                eventId,
                title,
                selectedDate,
                dayIndex,
                startHour,
                endHour - startHour,
                "event-blue",
                "dot-blue"
        );

        customEvents.computeIfAbsent(selectedDate, ignored -> new ArrayList<>()).add(newEvent);
        events = buildWeekEvents(weekStart);
        selectDate(selectedDate);

        eventTitleField.clear();
        eventStartField.clear();
        eventEndField.clear();
        showEventFeedback("Event added for " + selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM")) + ".", false);
    }

    private void showWeek(LocalDate newWeekStart, DayOfWeek preferredDay) {
        weekStart = newWeekStart.with(DayOfWeek.MONDAY);
        events = buildWeekEvents(weekStart);

        selectedDate = weekStart.plusDays(Math.max(0, Math.min(4, preferredDay.getValue() - 1)));
        if (weekStart.equals(today.with(DayOfWeek.MONDAY)) && today.getDayOfWeek().getValue() <= 5) {
            selectedDate = weekStart.plusDays(today.getDayOfWeek().getValue() - 1);
        }

        currentWeekLabel.setText(formatWeekRange(weekStart));
        buildWeekHeader();
        selectDate(selectedDate);
    }

    private void buildWeekHeader() {
        weekdayHeader.getChildren().clear();
        dayCards.clear();

        for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
            LocalDate date = weekStart.plusDays(dayIndex);

            Label dayName = new Label(date.format(HEADER_DAY_FMT).toUpperCase(Locale.ENGLISH));
            dayName.getStyleClass().add("weekday-name");

            Label dayDate = new Label(date.format(HEADER_DATE_FMT));
            dayDate.getStyleClass().add("weekday-date");

            VBox card = new VBox(2, dayName, dayDate);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(12, 8, 12, 8));
            card.getStyleClass().add("weekday-card");
            card.setOnMouseClicked(event -> selectDate(date));

            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            dayCards.add(card);
            weekdayHeader.getChildren().add(card);
        }
    }

    private void buildMonthHeader() {
        monthHeaderGrid.getChildren().clear();
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < days.length; i++) {
            Label label = new Label(days[i]);
            label.getStyleClass().add("mini-calendar-weekday");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            GridPane.setHgrow(label, Priority.ALWAYS);
            monthHeaderGrid.add(label, i, 0);
        }
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        refreshSelectedDayState();
        populateDaySchedule(date);
        populateMiniCalendar();
    }

    private void refreshSelectedDayState() {
        int selectedIndex = Math.max(0, Math.min(4, selectedDate.getDayOfWeek().getValue() - 1));

        for (int i = 0; i < dayCards.size(); i++) {
            VBox card = dayCards.get(i);
            if (i == selectedIndex) {
                if (!card.getStyleClass().contains("weekday-card-active")) {
                    card.getStyleClass().add("weekday-card-active");
                }
            } else {
                card.getStyleClass().remove("weekday-card-active");
            }
        }

        dayPanelTitleLabel.setText(selectedDate.equals(today) ? "Today" : selectedDate.format(DateTimeFormatter.ofPattern("EEEE")));
        todayDateLabel.setText(selectedDate.format(DAY_FMT));
        todayWeekLabel.setText("Week " + selectedDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()));
        agendaTitleLabel.setText("Month Overview");
    }

    private void populateDaySchedule(LocalDate date) {
        weeklyDayScheduleBox.getChildren().clear();
        weeklyDayCaptionLabel.setText(date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")));

        for (ScheduleEvent event : getEventsForDate(date)) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().addAll("weekly-day-card", mapDayScheduleClass(event.styleClass()));

            VBox timeBox = new VBox(2);
            timeBox.getStyleClass().add("weekly-day-card-timebox");
            Label start = new Label(event.startTime());
            start.getStyleClass().add("weekly-day-card-start");
            Label end = new Label(event.endTime());
            end.getStyleClass().add("weekly-day-card-end");
            timeBox.getChildren().addAll(start, end);

            VBox content = new VBox(3);
            Label title = new Label(event.title());
            title.getStyleClass().add("weekly-day-card-title");
            Label time = new Label(event.timeRange());
            time.getStyleClass().add("weekly-day-card-subtitle");
            content.getChildren().addAll(title, time);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("weekly-day-delete-btn");
            deleteButton.setOnAction(ignored -> deleteEvent(event));

            row.getChildren().addAll(timeBox, content, spacer, deleteButton);
            weeklyDayScheduleBox.getChildren().add(row);
        }
    }

    private void populateMiniCalendar() {
        monthGrid.getChildren().clear();

        YearMonth month = YearMonth.from(selectedDate);
        monthTitleLabel.setText(month.atDay(1).format(MONTH_FMT));

        LocalDate firstOfMonth = month.atDay(1);
        int startColumn = firstOfMonth.getDayOfWeek().getValue() - 1;

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            int index = startColumn + day - 1;
            int col = index % 7;
            int row = index / 7;

            Label label = new Label(String.valueOf(day));
            label.getStyleClass().add("mini-calendar-day");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            label.setOnMouseClicked(event -> {
                if (date.getDayOfWeek().getValue() <= 5) {
                    showWeek(date.with(DayOfWeek.MONDAY), date.getDayOfWeek());
                }
            });

            boolean isCurrentWeekDay = !date.isBefore(weekStart) && !date.isAfter(weekStart.plusDays(4));
            if (!isCurrentWeekDay) {
                label.getStyleClass().add("mini-calendar-day-muted");
            }
            if (date.equals(today)) {
                label.getStyleClass().add("mini-calendar-day-today");
            }
            if (date.equals(selectedDate)) {
                label.getStyleClass().add("mini-calendar-day-selected");
            }
            if (hasEventsOn(date)) {
                label.getStyleClass().add("mini-calendar-day-event");
            }

            GridPane.setHgrow(label, Priority.ALWAYS);
            monthGrid.add(label, col, row);
        }
    }

    private void deleteEvent(ScheduleEvent event) {
        hiddenEventIds.add(event.eventId());
        List<ScheduleEvent> customForDate = customEvents.get(event.date());
        if (customForDate != null) {
            customForDate.removeIf(item -> item.eventId().equals(event.eventId()));
            if (customForDate.isEmpty()) {
                customEvents.remove(event.date());
            }
        }
        events = buildWeekEvents(weekStart);
        selectDate(selectedDate);
    }

    private boolean hasEventsOn(LocalDate date) {
        return events.stream().anyMatch(event -> event.date().equals(date));
    }

    private List<ScheduleEvent> getEventsForDate(LocalDate date) {
        return new ArrayList<>(events.stream()
                .filter(event -> event.date().equals(date))
                .sorted(Comparator.comparingDouble(ScheduleEvent::startHour))
                .toList());
    }

    private List<ScheduleEvent> buildWeekEvents(LocalDate monday) {
        List<ScheduleEvent> combined = new ArrayList<>();
        combined.addAll(buildSampleEvents(monday));
        for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
            LocalDate date = monday.plusDays(dayIndex);
            combined.addAll(customEvents.getOrDefault(date, List.of()));
        }
        combined.removeIf(event -> hiddenEventIds.contains(event.eventId()));
        return combined;
    }

    private String mapDayScheduleClass(String eventClass) {
        return switch (eventClass) {
            case "event-blue" -> "weekly-day-accent-blue";
            case "event-green" -> "weekly-day-accent-green";
            case "event-red" -> "weekly-day-accent-red";
            case "event-purple" -> "weekly-day-accent-purple";
            case "event-gold" -> "weekly-day-accent-gold";
            default -> "weekly-day-accent-blue";
        };
    }

    private String formatWeekRange(LocalDate monday) {
        LocalDate friday = monday.plusDays(4);
        return monday.format(WEEK_RANGE_FMT) + " - " + friday.format(WEEK_RANGE_FMT);
    }

    private List<ScheduleEvent> buildSampleEvents(LocalDate monday) {
        int weekOffset = (int) ChronoUnit.WEEKS.between(today.with(DayOfWeek.MONDAY), monday);

        return List.of(
                ScheduleEvent.fromWeek("sample-ifb398-" + monday, weekOffset % 2 == 0 ? "IFB398 lecture" : "IFB398 lab", monday, 0, 16.0, 1.5, "event-red", "dot-red"),
                ScheduleEvent.fromWeek("sample-cab302-wed-" + monday, "CAB302 workshop", monday, 2, 10.0, 2.0, "event-blue", "dot-blue"),
                ScheduleEvent.fromWeek("sample-team-" + monday, weekOffset % 3 == 0 ? "Team meeting" : "Project sync", monday, 2, 19.0, 1.0, "event-gold", "dot-gold"),
                ScheduleEvent.fromWeek("sample-cab302-thu-" + monday, "CAB302 workshop", monday, 3, 10.0, 2.0, "event-blue", "dot-blue"),
                ScheduleEvent.fromWeek("sample-ifb220-lecture-" + monday, "IFB220 lecture", monday, 3, 12.0, 1.5, "event-green", "dot-green"),
                ScheduleEvent.fromWeek("sample-iab207-" + monday, weekOffset % 2 == 0 ? "IAB207 tutorial" : "IAB207 review", monday, 3, 16.0, 1.5, "event-purple", "dot-purple"),
                ScheduleEvent.fromWeek("sample-ifb220-tut-" + monday, "IFB220 tutorial", monday, 4, 16.0, 2.0, "event-green", "dot-green")
        );
    }

    private Double parseTime(String input) {
        String normalized = input.trim().toUpperCase(Locale.ENGLISH).replaceAll("\\s+", "");
        boolean pm = normalized.endsWith("PM");
        boolean am = normalized.endsWith("AM");
        if (pm || am) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }

        String[] parts = normalized.split(":");
        try {
            int hour;
            int minute;
            if (parts.length == 1) {
                hour = Integer.parseInt(parts[0]);
                minute = 0;
            } else if (parts.length == 2) {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } else {
                return null;
            }

            if (pm || am) {
                if (hour < 1 || hour > 12) {
                    return null;
                }
                if (pm && hour != 12) {
                    hour += 12;
                }
                if (am && hour == 12) {
                    hour = 0;
                }
            }

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return hour + (minute / 60.0);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void refreshGoogleConnectionUi() {
        googleCalendarButton.setDisable(false);
        googleCalendarButton.setText("Open Google Calendar");
        googleCalendarStatusLabel.setText("Opens Google Calendar in your browser");
        googleCalendarStatusLabel.getStyleClass().removeAll("calendar-connect-status-ok", "calendar-connect-status-off");
        googleCalendarStatusLabel.getStyleClass().add("calendar-connect-status-off");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Google Calendar");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showEventFeedback(String message, boolean isError) {
        eventFeedbackLabel.setText(message);
        eventFeedbackLabel.getStyleClass().removeAll("weekly-day-feedback-error", "weekly-day-feedback-success");
        eventFeedbackLabel.getStyleClass().add(isError ? "weekly-day-feedback-error" : "weekly-day-feedback-success");
        eventFeedbackLabel.setVisible(true);
        eventFeedbackLabel.setManaged(true);
    }

    private String styleForIndex(int index) {
        return switch (index % 5) {
            case 1 -> "event-green";
            case 2 -> "event-red";
            case 3 -> "event-purple";
            case 4 -> "event-gold";
            default -> "event-blue";
        };
    }

    private String dotForIndex(int index) {
        return switch (index % 5) {
            case 1 -> "dot-green";
            case 2 -> "dot-red";
            case 3 -> "dot-purple";
            case 4 -> "dot-gold";
            default -> "dot-blue";
        };
    }

    private record ScheduleEvent(
            String eventId,
            String title,
            LocalDate date,
            int dayIndex,
            double startHour,
            double durationHours,
            String styleClass,
            String dotClass
    ) {
        private static ScheduleEvent fromWeek(String eventId, String title, LocalDate weekStart, int dayIndex, double startHour,
                                              double durationHours, String styleClass, String dotClass) {
            return new ScheduleEvent(
                    eventId,
                    title,
                    weekStart.plusDays(dayIndex),
                    dayIndex,
                    startHour,
                    durationHours,
                    styleClass,
                    dotClass
            );
        }

        private String timeRange() {
            return format(startHour) + " - " + format(startHour + durationHours);
        }

        private String startTime() {
            return format(startHour);
        }

        private String endTime() {
            return format(startHour + durationHours);
        }

        private static String format(double value) {
            int hour = (int) value;
            int minutes = (int) Math.round((value - hour) * 60);
            int normalized = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            String suffix = hour >= 12 ? "PM" : "AM";
            return String.format("%d:%02d %s", normalized, minutes, suffix);
        }
    }
}
