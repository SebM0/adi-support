package com.axway.adi.tools;

import java.util.*;
import com.axway.adi.tools.util.db.DbConstants.ResourceType;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCase;
import javafx.event.Event;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import static com.axway.adi.tools.DisturbMain.MAIN;
import static com.axway.adi.tools.util.DiagnosticCatalog.CAT;
import static com.axway.adi.tools.util.db.DbConstants.Status.InProgress;

public class DisturbWelcomeController extends AbstractController {
    public ListView<SupportCase> supportInProgressList;
    public TableView<Map<String, Object>> diagTable;

    void loadData() {
        CAT.getSupportCasesByStatus(InProgress).forEach(sc -> supportInProgressList.getItems().add(sc));
        ResourceType.concrete().forEach(rt -> {
            List<DiagnosticSpecification> diagnostics = CAT.getDiagnosticsByType(rt);
            int customs = (int)diagnostics.stream().filter(DiagnosticSpecification::isCustom).count();
            diagTable.getItems().add(Map.of("name", rt.name(), "builtin", diagnostics.size() - customs, "custom", customs));
        });
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
