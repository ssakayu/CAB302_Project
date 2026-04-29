package com.focusdesk.controller;

import com.focusdesk.app.Session;
import com.focusdesk.dao.NoteDAO;
import com.focusdesk.model.Note;
import com.focusdesk.util.TaskRunner;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotesController {

    @FXML private FlowPane noteGrid;
    @FXML private TextField searchField;
    @FXML private VBox modalOverlay;
    @FXML private Label modalTitleLabel;
    @FXML private TextArea noteContent;
    @FXML private Button deleteBtn;

    // Pastel card colours — background, foreground text, date text
    private static final String[] CARD_BG  = {"#fef9c3", "#dbeafe", "#ede9fe", "#dcfce7", "#fce7f3"};
    private static final String[] CARD_FG  = {"#713f12", "#1e3a8a", "#4c1d95", "#14532d", "#831843"};
    private static final String[] CARD_SUB = {"#92400e", "#1d4ed8", "#6d28d9", "#15803d", "#9d174d"};

    private static final DateTimeFormatter DB_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CARD_FMT = DateTimeFormatter.ofPattern("EEE d MMM");

    private final NoteDAO dao = new NoteDAO();
    private final DropShadow hoverShadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.22));

    private Note editingNote = null;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        loadNotes(null);
    }

    // -------------------------------------------------------------------------
    // Load / search
    // -------------------------------------------------------------------------

    private void loadNotes(String query) {
        int userId = Session.get().getCurrentUser().getId();
        TaskRunner.run(
                () -> (query == null || query.isBlank())
                        ? dao.getAll(userId)
                        : dao.search(userId, query),
                this::populateGrid,
                err -> System.err.println("Notes load error: " + err.getMessage())
        );
    }

    private void populateGrid(List<Note> notes) {
        noteGrid.getChildren().clear();
        if (notes.isEmpty()) {
            Label empty = new Label("No notes yet. Click '+ New Note' to get started.");
            empty.getStyleClass().add("notes-empty-label");
            noteGrid.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < notes.size(); i++) {
            noteGrid.getChildren().add(buildCard(notes.get(i), i));
        }
    }

    @FXML
    private void onSearch() {
        loadNotes(searchField.getText().trim());
    }

    // -------------------------------------------------------------------------
    // Card builder
    // -------------------------------------------------------------------------

    private Node buildCard(Note note, int index) {
        int c = index % CARD_BG.length;

        Label content = new Label(preview(note.getContent()));
        content.setWrapText(true);
        content.getStyleClass().add("note-card-content");
        content.setStyle("-fx-text-fill: " + CARD_FG[c] + ";");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label date = new Label(formatDate(note.getCreatedAt()));
        date.getStyleClass().add("note-card-date");
        date.setStyle("-fx-text-fill: " + CARD_SUB[c] + ";");

        VBox card = new VBox(8, content, spacer, date);
        card.getStyleClass().add("note-card");
        card.setStyle("-fx-background-color: " + CARD_BG[c] + ";");
        card.setPrefWidth(190);

        card.setOnMouseEntered(e -> card.setEffect(hoverShadow));
        card.setOnMouseExited(e -> card.setEffect(null));
        card.setOnMouseClicked(e -> openModal(note));

        return card;
    }

    private String preview(String content) {
        if (content == null) return "";
        return content.length() > 130 ? content.substring(0, 130) + "…" : content;
    }

    private String formatDate(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) return "";
        try {
            return LocalDateTime.parse(createdAt, DB_FMT).format(CARD_FMT);
        } catch (Exception e) {
            return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
        }
    }

    // -------------------------------------------------------------------------
    // Modal open / close
    // -------------------------------------------------------------------------

    @FXML
    private void onNewNote() {
        editingNote = null;
        noteContent.clear();
        modalTitleLabel.setText("New Note");
        deleteBtn.setVisible(false);
        deleteBtn.setManaged(false);
        showModal(true);
    }

    private void openModal(Note note) {
        editingNote = note;
        noteContent.setText(note.getContent());
        modalTitleLabel.setText("Edit Note");
        deleteBtn.setVisible(true);
        deleteBtn.setManaged(true);
        showModal(true);
    }

    @FXML
    private void onModalClose() {
        showModal(false);
    }

    private void showModal(boolean show) {
        modalOverlay.setVisible(show);
        modalOverlay.setManaged(show);
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    @FXML
    private void onSave() {
        String text = noteContent.getText().trim();
        if (text.isEmpty()) return;

        int userId = Session.get().getCurrentUser().getId();
        final Note target = editingNote;

        TaskRunner.run(
                () -> {
                    if (target == null) dao.insert(userId, text);
                    else                dao.update(target.getId(), text);
                    return null;
                },
                ignored -> {
                    showModal(false);
                    loadNotes(searchField.getText().trim());
                },
                err -> System.err.println("Save failed: " + err.getMessage())
        );
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @FXML
    private void onDelete() {
        if (editingNote == null) return;
        int noteId = editingNote.getId();

        TaskRunner.run(
                () -> { dao.delete(noteId); return null; },
                ignored -> {
                    showModal(false);
                    loadNotes(searchField.getText().trim());
                },
                err -> System.err.println("Delete failed: " + err.getMessage())
        );
    }
}
