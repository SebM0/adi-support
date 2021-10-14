package com.axway.adi.tools.disturb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

public class SingleFileParserMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();

        //ThreadDumpController controller = new ThreadDumpController(primaryStage);
        //loader.setController(controller);
        CAT.load();

        loader.setLocation(getClass().getResource("singlefileparser.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("Single file parser");
        primaryStage.setScene(new Scene(root, 1000, 600));
        loader.<SingleFileParserController>getController().bindControls(primaryStage);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
