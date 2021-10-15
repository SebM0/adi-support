package com.axway.adi.tools.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import static javafx.application.Platform.*;

public class AlertHelper {
    private AlertHelper() {}

    public static void show(AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Alert");
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (isFxApplicationThread()) {
            alert.showAndWait();
        } else {
            runLater(alert::showAndWait);
        }
    }
}
