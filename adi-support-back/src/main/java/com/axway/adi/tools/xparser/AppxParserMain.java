package com.axway.adi.tools.xparser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppxParserMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();

        AppxController controller = new AppxController();
        loader.setController(controller);

        loader.setLocation(getClass().getResource("xparser.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("Application parser");
        primaryStage.setScene(new Scene(root, 630, 400));
        controller.bindControls();
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
