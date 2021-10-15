package com.axway.adi.tools.redolog;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RedoLogMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();

        RedoLogController controller = new RedoLogController(primaryStage);
        loader.setController(controller);

        loader.setLocation(getClass().getResource("redolog.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("RedoLog parser");
        primaryStage.setScene(new Scene(root, 630, 400));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
