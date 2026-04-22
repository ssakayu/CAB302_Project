package com.focusdesk.app;

import com.focusdesk.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.init();

        Session.getInstance().setMainStage(stage);

        // TODO: restore login.fxml once auth is implemented
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setTitle("FocusDesk");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();

        // Widget is NOT opened here — it opens on user action via MainController
    }
}
