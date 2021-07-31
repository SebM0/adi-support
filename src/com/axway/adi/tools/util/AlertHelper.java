package com.axway.adi.tools.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class AlertHelper {
    private AlertHelper() {}

    public static void show(AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Alert");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}
