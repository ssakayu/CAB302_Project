package com.focusdesk.app;

import com.focusdesk.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws Exception {
        // init DB + tables
        DatabaseManager.init();

        scene = new Scene(loadFXML("login"), 420, 520);
        stage.setTitle("FocusDesk - Auth");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws Exception {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        return loader.load();
    }
}