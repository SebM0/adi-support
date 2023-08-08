package com.axway.adi.tools.disturb;

import java.io.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

public class SingleFileParserMain extends Application {
    static SingleFileParserMain MAIN;
    private Stage stage;
    private Scene mainScene;
    private SingleFileParserController mainController;
    private Scene detailScene;
    private DisturbDetailsController detailController;

    @Override
    public void start(Stage primaryStage) throws Exception{
        MAIN = this;
        stage = primaryStage;
        CAT.load();

        loadMainScene();
        loadDetailScene();

        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Single file parser");
        primaryStage.show();
    }

    private void loadMainScene() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("singlefileparser.fxml"));
        mainScene = new Scene(loader.load(), 1000, 800);
        mainController = loader.getController();
        mainController.bindControls(stage);
    }

    private void loadDetailScene() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("disturbDetails.fxml"));
        detailScene = new Scene(loader.load(), 1000, 800);
        detailController = loader.getController();
        detailController.setClosure(a -> stage.setScene(mainScene));
    }

    void showDiagnosticDetails(TableView.TableViewSelectionModel<DiagnosticResult> diagnosticResults) {
        detailController.setResult(diagnosticResults);
        stage.setScene(detailScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
