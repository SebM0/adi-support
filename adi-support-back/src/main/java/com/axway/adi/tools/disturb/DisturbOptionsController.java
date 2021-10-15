package com.axway.adi.tools.disturb;

import com.axway.adi.tools.AbstractController;
import javafx.event.Event;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

public class DisturbOptionsController extends AbstractController {
    public ListView<String> supportInProgressList;

    public void onSupportInProgress(Event event) {
        // TODO implement method
    }

    public void onSupportInProgressClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2 && (mouseEvent.getTarget() instanceof GridPane
                || ((GridPane) mouseEvent.getTarget()).getChildren().size() > 0)) {

            //your code for double click handling here
        }
    }

    public void onSupportClosed(Event event) {
        // TODO implement method
    }
}
