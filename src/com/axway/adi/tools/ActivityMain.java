package com.axway.adi.tools;

import java.io.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ActivityMain extends Application {
    private ActivityController controller;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        controller = new ActivityController(primaryStage);
        loader.setController(controller);

        loader.setLocation(getClass().getResource("activity.fxml"));

        Parent root = loader.load();
        controller.bindControls();
        primaryStage.setTitle("Activity summary viewer");
        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.show();


        new Thread(() -> {
            try {
                launchParser(controller, running);
            } catch (IOException e) {
                throw new RuntimeException(e);  // TODO handle exception
            }
        }).start();
    }

    @Override
    public void stop() {
        running.set(false);
    }

    private static void launchParser(ActivityController controller, AtomicBoolean running) throws IOException {
        boolean init = true;
        try (FileInputStream fstream = new FileInputStream(
                "S:\\sources\\hw-202107\\hvp\\profile-hvp-application\\.runner\\node-1\\var\\log\\internal-summary-memory.log")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while (running.get()) {
                line = br.readLine();
                if (line == null) {
                    if (init) {
                        controller.updateView();
                        init = false;
                    }
                    Thread.sleep(500);
                } else {
                    controller.insertLine(line, init);
                }
            }
        } catch (InterruptedException e) {
            // stop
        }
    }
}
