package com.axway.adi.tools;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ActivityMain extends Application {
    private static final int WAITING_NEW = 500;
    private static final int WAITING_EXISTING = 200;
    private ActivityController controller;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean playing = new AtomicBoolean(true);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("activity.fxml"));

        primaryStage.setTitle("Activity summary viewer");
        primaryStage.setScene(new Scene(loader.load(), 1000, 600));
        controller = loader.getController();

        controller.bindControls(this);
        primaryStage.show();

//        Path runnerPath = Path.of("S:\\sources\\tornado2\\hvp\\profile-hvp-application\\.runner\\node-1");
        Path runnerPath = Path.of("C:\\QA\\BUGS\\15851\\micro-177\\archive\\application");

        new Thread(() -> {
            try {
                launchParser(runnerPath.resolve("var\\log\\internal-summary-memory.log"), controller, running, playing);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(() -> {
            try {
                launchParser(runnerPath.resolve("var\\log\\internal-runtime-activity.log"), controller, running, playing);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public void stop() {
        running.set(false);
    }

    public void setPlaying(boolean value) {
        playing.set(value);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    private static void launchParser(Path logFile, ActivityController controller, AtomicBoolean running, AtomicBoolean playing) throws IOException {
        try (FileInputStream fstream = new FileInputStream(logFile.toFile())) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while (running.get()) {
                if (!playing.get()) {
                    Thread.sleep(WAITING_NEW);
                    continue;
                }
                line = br.readLine();
                if (line == null) {
                    Thread.sleep(WAITING_NEW);
                } else {
                    if (WAITING_EXISTING > 0) {
                        Thread.sleep(WAITING_EXISTING);
                    }
                    controller.insertLine(line);
                }
            }
        } catch (InterruptedException e) {
            // stop
        }
    }
}
