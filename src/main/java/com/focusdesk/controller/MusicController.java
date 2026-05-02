package com.focusdesk.controller;

import com.focusdesk.app.Session;
import com.focusdesk.dao.OAuthTokenDAO;
import com.focusdesk.service.SpotifyService;
import com.focusdesk.util.AppConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class MusicController {

    @FXML private Label spotifyStatusLabel;

    @FXML private Label trackTitleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label playbackLabel;

    @FXML private ImageView albumCoverView;

    @FXML private Button setClientIdBtn;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;
    @FXML private Button refreshBtn;

    @FXML private Button playPauseBtn;

    private final SpotifyService spotify = new SpotifyService();
    private final OAuthTokenDAO tokenDAO = new OAuthTokenDAO();

    private Timeline uiTick;   // every 1s local progress
    private Timeline apiPoll;  // every ~12s Spotify API refresh

    private volatile boolean isPlaying = false;
    private volatile long progressMs = 0;
    private volatile long durationMs = 0;

    @FXML
    private void initialize() {
        // stop timers if this view is removed
        spotifyStatusLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) stopTimers();
        });

        var user = Session.get().getCurrentUser();
        if (user == null) {
            setDisconnectedUI("Spotify: please login first.");
            return;
        }

        if (AppConfig.getSpotifyClientId().isBlank()) {
            setDisconnectedUI("Spotify: set Client ID first.");
            return;
        }

        spotifyStatusLabel.setText("Spotify: checking connection...");

        new Thread(() -> {
            try {
                int userId = user.getId();
                var me = spotify.getMe(userId);
                String name = me.get("display_name").getAsString();

                Platform.runLater(() -> {
                    setConnectedUI(name);
                    startTimers(userId);
                    refreshNowPlayingAsync(userId);
                });

            } catch (Exception e) {
                Platform.runLater(() -> setDisconnectedUI("Spotify: not connected."));
            }
        }).start();
    }

    // ---------------- Buttons ----------------

    @FXML
    private void onSetSpotifyClientId() {
        TextInputDialog d = new TextInputDialog(AppConfig.getSpotifyClientId());
        d.setTitle("Spotify Setup");
        d.setHeaderText("Paste your Spotify Client ID (Spotify Developer Dashboard)");
        d.setContentText("Client ID:");

        d.showAndWait().ifPresent(id -> {
            if (id != null && !id.trim().isBlank()) {
                AppConfig.setSpotifyClientId(id.trim());
                spotifyStatusLabel.setText("Spotify: Client ID saved ✅");
                connectBtn.setDisable(false);
            }
        });
    }

    @FXML
    private void onConnectSpotify() {
        var user = Session.get().getCurrentUser();
        if (user == null) {
            setDisconnectedUI("Spotify: please login first.");
            return;
        }
        if (AppConfig.getSpotifyClientId().isBlank()) {
            setDisconnectedUI("Spotify: set Client ID first.");
            return;
        }

        spotifyStatusLabel.setText("Spotify: opening browser...");
        connectBtn.setDisable(true);

        int userId = user.getId();
        new Thread(() -> {
            try {
                spotify.connect(userId);

                var me = spotify.getMe(userId);
                String name = me.get("display_name").getAsString();

                Platform.runLater(() -> {
                    setConnectedUI(name);
                    startTimers(userId);
                    refreshNowPlayingAsync(userId);
                });

            } catch (Exception e) {
                Platform.runLater(() -> setDisconnectedUI("Spotify error: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> connectBtn.setDisable(false));
            }
        }).start();
    }

    @FXML
    private void onDisconnectSpotify() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;

        int userId = user.getId();

        new Thread(() -> {
            try {
                tokenDAO.delete(userId, "spotify");
                Platform.runLater(() -> {
                    stopTimers();
                    setDisconnectedUI("Spotify: disconnected.");
                    clearNowPlaying();
                });
            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onRefreshNowPlaying() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;
        refreshNowPlayingAsync(user.getId());
    }

    // -------- Playback controls --------

    @FXML
    private void onPlayPause() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;
        int userId = user.getId();

        new Thread(() -> {
            try {
                if (isPlaying) spotify.pause(userId);
                else spotify.play(userId);

                Thread.sleep(250);
                refreshNowPlayingAsync(userId);

            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onNextTrack() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;
        int userId = user.getId();

        new Thread(() -> {
            try {
                spotify.nextTrack(userId);
                Thread.sleep(400);
                refreshNowPlayingAsync(userId);
            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onPrevTrack() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;
        int userId = user.getId();

        new Thread(() -> {
            try {
                spotify.previousTrack(userId);
                Thread.sleep(400);
                refreshNowPlayingAsync(userId);
            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onRewind10() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;
        int userId = user.getId();

        long newPos = Math.max(0, progressMs - 10_000);

        new Thread(() -> {
            try {
                spotify.seekTo(userId, newPos);
                progressMs = newPos;
                Platform.runLater(() -> playbackLabel.setText((isPlaying ? "Playing" : "Paused") + " • " + msToMinSec(progressMs)));
            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onForward10() {
        var user = Session.get().getCurrentUser();
        if (user == null) return;
        int userId = user.getId();

        long newPos = Math.min(durationMs, progressMs + 10_000);

        new Thread(() -> {
            try {
                spotify.seekTo(userId, newPos);
                progressMs = newPos;
                Platform.runLater(() -> playbackLabel.setText((isPlaying ? "Playing" : "Paused") + " • " + msToMinSec(progressMs)));
            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    // ---------------- UI state ----------------

    private void setConnectedUI(String displayName) {
        spotifyStatusLabel.setText("Spotify: connected as " + displayName + " ✅");

        setClientIdBtn.setVisible(false);
        setClientIdBtn.setManaged(false);

        connectBtn.setVisible(false);
        connectBtn.setManaged(false);

        disconnectBtn.setVisible(true);
        disconnectBtn.setManaged(true);

        refreshBtn.setDisable(false);
    }

    private void setDisconnectedUI(String msg) {
        spotifyStatusLabel.setText(msg);

        setClientIdBtn.setVisible(true);
        setClientIdBtn.setManaged(true);

        connectBtn.setVisible(true);
        connectBtn.setManaged(true);
        connectBtn.setDisable(false);

        disconnectBtn.setVisible(false);
        disconnectBtn.setManaged(false);

        refreshBtn.setDisable(true);

        if (playPauseBtn != null) playPauseBtn.setText("⏯");
        stopTimers();
    }

    private void clearNowPlaying() {
        trackTitleLabel.setText("—");
        artistLabel.setText("—");
        albumLabel.setText("—");
        playbackLabel.setText("—");
        albumCoverView.setImage(null);
        isPlaying = false;
        progressMs = 0;
        durationMs = 0;
    }

    // ---------------- timers ----------------

    private void startTimers(int userId) {
        stopTimers();

        uiTick = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (isPlaying) {
                progressMs = Math.min(progressMs + 1000, durationMs);
                playbackLabel.setText("Playing • " + msToMinSec(progressMs));
            }
        }));
        uiTick.setCycleCount(Timeline.INDEFINITE);
        uiTick.play();

        apiPoll = new Timeline(new KeyFrame(Duration.seconds(12), e -> refreshNowPlayingAsync(userId)));
        apiPoll.setCycleCount(Timeline.INDEFINITE);
        apiPoll.play();
    }

    private void stopTimers() {
        if (uiTick != null) { uiTick.stop(); uiTick = null; }
        if (apiPoll != null) { apiPoll.stop(); apiPoll = null; }
    }

    // ---------------- spotify fetch ----------------

    private void refreshNowPlayingAsync(int userId) {
        new Thread(() -> {
            try {
                var playing = spotify.getCurrentlyPlaying(userId);

                if (playing == null) {
                    Platform.runLater(() -> {
                        trackTitleLabel.setText("(nothing playing)");
                        artistLabel.setText("—");
                        albumLabel.setText("—");
                        playbackLabel.setText("—");
                        albumCoverView.setImage(null);
                        isPlaying = false;
                        progressMs = 0;
                        durationMs = 0;
                        if (playPauseBtn != null) playPauseBtn.setText("▶ Play");
                    });
                    return;
                }

                boolean playingFlag = playing.has("is_playing") && playing.get("is_playing").getAsBoolean();
                long progress = playing.has("progress_ms") && !playing.get("progress_ms").isJsonNull()
                        ? playing.get("progress_ms").getAsLong()
                        : 0;

                var item = playing.getAsJsonObject("item");
                String trackName = item.get("name").getAsString();
                long duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0;

                var artistsArr = item.getAsJsonArray("artists");
                StringBuilder artists = new StringBuilder();
                for (int i = 0; i < artistsArr.size(); i++) {
                    if (i > 0) artists.append(", ");
                    artists.append(artistsArr.get(i).getAsJsonObject().get("name").getAsString());
                }

                var albumObj = item.getAsJsonObject("album");
                String albumName = albumObj.get("name").getAsString();

                String coverUrl = null;
                if (albumObj.has("images") && albumObj.getAsJsonArray("images").size() > 0) {
                    coverUrl = albumObj.getAsJsonArray("images").get(0).getAsJsonObject().get("url").getAsString();
                }

                isPlaying = playingFlag;
                progressMs = progress;
                durationMs = duration;

                String statusText = (playingFlag ? "Playing" : "Paused") + " • " + msToMinSec(progress);

                String finalCoverUrl = coverUrl;
                Platform.runLater(() -> {
                    trackTitleLabel.setText(trackName);
                    artistLabel.setText(artists.toString());
                    albumLabel.setText(albumName);
                    playbackLabel.setText(statusText);

                    if (playPauseBtn != null) playPauseBtn.setText(isPlaying ? "⏸ Pause" : "▶ Play");

                    if (finalCoverUrl != null && !finalCoverUrl.isBlank()) {
                        albumCoverView.setImage(new Image(finalCoverUrl, true));
                    } else {
                        albumCoverView.setImage(null);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> spotifyStatusLabel.setText("Spotify error: " + e.getMessage()));
            }
        }).start();
    }

    private String msToMinSec(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min + ":" + (sec < 10 ? "0" + sec : sec);
    }
}