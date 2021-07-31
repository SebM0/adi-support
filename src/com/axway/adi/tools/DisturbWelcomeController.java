package com.axway.adi.tools;

import com.axway.adi.tools.util.db.DbConstants;
import com.axway.adi.tools.util.db.SupportCase;
import javafx.event.Event;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import static com.axway.adi.tools.DisturbMain.MAIN;
import static com.axway.adi.tools.util.db.DbConstants.Status.InProgress;

public class DisturbWelcomeController extends AbstractController {
    public ListView<SupportCase> supportInProgressList;

    void loadData() {
        MAIN.CAT.getSupportCasesByStatus(InProgress).forEach(sc -> supportInProgressList.getItems().add(sc));
    }

    public void onSupportInProgress(Event event) {
        // TODO implement method
    }

    public void onSupportInProgressClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            //your code for double click handling here
            SupportCase selectedItem = supportInProgressList.getSelectionModel().getSelectedItem();
            if (selectedItem != null && mouseEvent.getTarget() != null && mouseEvent.getTarget().toString().contains(selectedItem.toString())) {
                //AlertHelper.show(INFORMATION, "Item " + selectedItem + " launched");
                MAIN.editSupportCase(selectedItem);
            }
        }
    }

    public void onSupportClosed(Event event) {
        // TODO implement method
    }
}
