package com.axway.adi.tools.activity;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ActivityMain extends Application {
    private Stage stage;
    private ActivityParser parser;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;

        primaryStage.setTitle("Activity summary viewer");
        parser = new ActivityParser();

        launchNodeSelectionScene();
        stage.show();
    }

    private void launchNodeSelectionScene() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("activityNodeSelector.fxml"));
        Scene scene = new Scene(loader.load());
        ActivityNodeSelector nodeSelector = loader.getController();
        nodeSelector.bindControls(this);
        stage.setScene(scene);
    }

    public void launchActivityScene(Path runnerPath) {
        stage.setTitle("Activity summary tracker - " + runnerPath);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("activity.fxml"));
        Scene scene;
        try {
            scene = new Scene(loader.load(), 1000, 600);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ActivityController controller = loader.getController();
        controller.bindControls(this);
        stage.setScene(scene);
        stage.centerOnScreen();

        parser.readActivity(controller, runnerPath);
    }

    public void launchComputingScene(Path runnerPath) {
        stage.setTitle("Computing tracker - " + runnerPath);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("computing.fxml"));
        Scene scene;
        try {
            scene = new Scene(loader.load(), 1000, 600);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ComputingController controller = loader.getController();
        controller.bindControls(running);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        running.set(false);
        parser.stop();
    }

    public void setPlaying(boolean value) {
        parser.setPlaying(value);
    }

    public boolean isPlaying() {
        return parser.isPlaying();
    }
}
