package com.focusdesk.app;

import com.focusdesk.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.init();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 420, 520);
        stage.setTitle("FocusDesk");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }
}
