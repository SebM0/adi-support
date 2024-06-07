package com.axway.adi.tools.activity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.util.AlertHelper;
import javafx.event.ActionEvent;
import javafx.scene.control.ComboBox;
import javafx.stage.DirectoryChooser;

import static javafx.scene.control.Alert.AlertType.ERROR;

public class ActivityNodeSelector {
    private static final String MRU = "LastFile";
    private static final String PROPERTIES = "activity.properties";
    public ComboBox<String> pathCombo;
    private final List<String> mruPaths = new ArrayList<>();
    private ActivityMain parent;

    void bindControls(ActivityMain activityMain) {
        this.parent = activityMain;
        loadProperties();
        pathCombo.getItems().addAll(mruPaths);
        if (!mruPaths.isEmpty()) {
            pathCombo.setValue(mruPaths.get(0));
        }
        pathCombo.setEditable(true);

//        pathCombo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
//            pathCombo.setValue(newText);
//        });
    }

    public void onBrowse(ActionEvent event) {
        event.consume();
        String value = pathCombo.getValue();
        DirectoryChooser fileChooser = new DirectoryChooser();
        if (!value.isBlank()) {
            File initialDirectory = new File(value);
            if (initialDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(initialDirectory);
            }
        }
        File file = fileChooser.showDialog(null);
        if (file != null) {
            pathCombo.setValue(file.getAbsolutePath());
        }
    }

    public void onLaunchActivity(ActionEvent event) {
        event.consume();
        Path runnerPath = checkRunnerPath();
        if (runnerPath != null) {
            parent.launchActivityScene(runnerPath);
        }
    }

    public void onLaunchComputing(ActionEvent event) {
        event.consume();
        Path runnerPath = checkRunnerPath();
        if (runnerPath != null) {
            parent.launchComputingScene(runnerPath);
        }
    }

    private Path checkRunnerPath() {
        String value = pathCombo.getValue();
        Path runnerPath = Path.of(value);
        if (!Files.isDirectory(runnerPath)) {
            AlertHelper.show(ERROR, value + " is not a folder");
            return null;
        }
        if (!ActivityParser.hasActivityFiles(runnerPath)) {
            AlertHelper.show(ERROR, value + " has no activity log");
            return null;
        }
        updateProperties(value);
        return runnerPath;
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (InputStream propertiesStream = Files.newInputStream(Path.of(PROPERTIES))) {
            properties.load(propertiesStream);
            for (int i = 1; i <= 10; i++) {
                String mru = properties.getProperty(MRU + "-" + i);
                if (mru != null && !mru.isEmpty()) {
                    mruPaths.add(mru);
                }
            }
        } catch (NoSuchFileException e) {
            // skip
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateProperties(String lastFile) {
        // last file in 1st position
        mruPaths.remove(lastFile);
        mruPaths.add(0, lastFile);
        Properties properties = new Properties();
        int i = 1;
        for (String mru : mruPaths) {
            if (mru != null && !mru.isEmpty()) {
                properties.setProperty(MRU + "-" + i++, mru);
            }
        }
        try (OutputStream propertiesStream = Files.newOutputStream(Path.of(PROPERTIES))) {
            properties.store(propertiesStream, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
