package com.axway.adi.tools.disturb;

import java.util.*;
import com.axway.adi.tools.AbstractController;
import com.axway.adi.tools.disturb.db.DbConstants.ResourceType;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCase;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static com.axway.adi.tools.disturb.DisturbMain.MAIN;
import static com.axway.adi.tools.disturb.db.DbConstants.Status.InProgress;

public class DisturbWelcomeController extends AbstractController {
    public ListView<SupportCase> supportInProgressList;
    public TableView<Map<String, Object>> diagTable;
    private ContextMenu itemContextMenu = new ContextMenu();
    private ContextMenu emptyContextMenu = new ContextMenu();

    @Override
    protected void bindControls(Stage parentStage)
    {
        super.bindControls(parentStage);
        MenuItem item1 = new MenuItem("Edit case");
        item1.setOnAction(event -> editCase(supportInProgressList.getSelectionModel().getSelectedItem()));
        MenuItem item2 = new MenuItem("Delete case");
        item2.setOnAction(event -> deleteCase(supportInProgressList.getSelectionModel().getSelectedItem()));
        MenuItem item3 = new MenuItem("New case");
        item3.setOnAction(event -> newCase());
        MenuItem item4 = new MenuItem("New case");
        item4.setOnAction(event -> newCase());
        itemContextMenu.getItems().addAll(item1, item2, new SeparatorMenuItem(), item3);
        emptyContextMenu.getItems().add(item4);
        //supportInProgressList.setOnContextMenuRequested(event -> supportInProgressList.setContextMenu(itemContextMenu)/*.show(supportInProgressList, event.getScreenX(), event.getScreenY())*/);
    }

    void loadData() {
        CAT.getSupportCasesByStatus(InProgress).forEach(sc -> supportInProgressList.getItems().add(sc));
        ResourceType.concrete().forEach(rt -> {
            List<DiagnosticSpecification> diagnostics = CAT.getDiagnosticsByType(rt);
            if (!diagnostics.isEmpty()) {
                diagTable.getItems().add(Map.of("name", rt.name(), "count", diagnostics.size()));
            }
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
                editCase(selectedItem);
                mouseEvent.consume();
            }
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            SupportCase selectedItem = supportInProgressList.getSelectionModel().getSelectedItem();
            if (selectedItem != null && mouseEvent.getTarget() != null && mouseEvent.getTarget().toString().contains(selectedItem.toString())) {
                supportInProgressList.setContextMenu(itemContextMenu);
            } else {
                supportInProgressList.setContextMenu(emptyContextMenu);
            }
            mouseEvent.consume();
        }
    }

    public void onSupportInProgressKey(KeyEvent keyEvent) {
        SupportCase selectedItem = supportInProgressList.getSelectionModel().getSelectedItem();
        if (selectedItem == null)
            return;
        if ("N".equals(keyEvent.getCode().getChar()) && keyEvent.isControlDown()) {
            newCase();
            keyEvent.consume();
        } else if (keyEvent.getCode().getCode() == 127) { //DELETE
            deleteCase(selectedItem);
            keyEvent.consume();
        } else if (keyEvent.getCode().getCode() == 10) { //ENTER
            editCase(selectedItem);
            keyEvent.consume();
        }
    }

    public void onSupportClosed(Event event) {
        // TODO implement method
    }

    private void editCase(SupportCase selectedItem) {
        //AlertHelper.show(INFORMATION, "Item " + selectedItem + " launched");
        if (selectedItem != null)
            MAIN.editSupportCase(selectedItem);
    }

    private void deleteCase(SupportCase selectedItem) {
        if (selectedItem != null) {
            CAT.deleteSupportCase(selectedItem);
            supportInProgressList.getItems().remove(selectedItem);
        }
    }

    private void newCase() {
        SupportCase selectedItem = CAT.createSupportCases("TORNADO-xxx");
        editCase(selectedItem);
    }
}
