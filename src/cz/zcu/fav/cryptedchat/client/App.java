package cz.zcu.fav.cryptedchat.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
            "main.fxml"));
        Parent root = loader.load();
        OnCloseListener closeListener = loader.getController();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setWidth(600);
        primaryStage.setHeight(400);
        primaryStage.setOnCloseRequest(event -> closeListener.onClose());
        primaryStage.show();
    }

    public interface OnCloseListener {
        void onClose();
    }
}
