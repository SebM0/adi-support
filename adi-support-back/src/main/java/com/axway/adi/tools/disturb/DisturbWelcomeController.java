package com.axway.adi.tools.disturb;

import java.util.*;
import com.axway.adi.tools.AbstractController;
import com.axway.adi.tools.disturb.db.DbConstants.ResourceType;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCase;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static com.axway.adi.tools.disturb.DisturbMain.MAIN;
import static com.axway.adi.tools.disturb.db.DbConstants.Status.InProgress;

public class DisturbWelcomeController extends AbstractController {
    public TableView<Map<String, Object>> diagTable;
    public TableView<SupportCase> caseTable;
    private final ContextMenu itemContextMenu = new ContextMenu();
    private final ContextMenu emptyContextMenu = new ContextMenu();

    @SuppressWarnings("unchecked")
    @Override
    protected void bindControls(Stage parentStage)
    {
        super.bindControls(parentStage);
        // Bind caseTable
        {
            ObservableList<TableColumn<SupportCase, ?>> columns = caseTable.getColumns();
            TableColumn<SupportCase, String> nameColumn = (TableColumn<SupportCase, String>) columns.get(0);
            nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().toString()));
            TableColumn<SupportCase, String> dateColumn = (TableColumn<SupportCase, String>) columns.get(1);
            dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getLastExecution()));
            caseTable.getSortOrder().add(nameColumn);
        }
        // Bind diagTable
        {
            ObservableList<TableColumn<Map<String, Object>, ?>> columns = diagTable.getColumns();
            TableColumn<Map<String, Object>, String> nameColumn = (TableColumn<Map<String, Object>, String>) columns.get(0);
            diagTable.getSortOrder().add(nameColumn);
        }
        // Bind contextual menus
        MenuItem item1 = new MenuItem("Edit case");
        item1.setOnAction(event -> editCase(caseTable.getSelectionModel().getSelectedItem()));
        MenuItem item2 = new MenuItem("Delete case");
        item2.setOnAction(event -> deleteCase(caseTable.getSelectionModel().getSelectedItem()));
        MenuItem item3 = new MenuItem("New case");
        item3.setOnAction(event -> newCase());
        MenuItem item4 = new MenuItem("New case");
        item4.setOnAction(event -> newCase());
        itemContextMenu.getItems().addAll(item1, item2, new SeparatorMenuItem(), item3);
        emptyContextMenu.getItems().add(item4);
        //supportInProgressList.setOnContextMenuRequested(event -> supportInProgressList.setContextMenu(itemContextMenu)/*.show(supportInProgressList, event.getScreenX(), event.getScreenY())*/);
    }

    void loadData() {
        // clear
        caseTable.getItems().clear();
        diagTable.getItems().clear();
        // load
        CAT.getSupportCasesByStatus(InProgress).forEach(sc -> caseTable.getItems().add(sc));
        ResourceType.concrete().forEach(rt -> {
            List<DiagnosticSpecification> diagnostics = CAT.getDiagnosticsByType(rt);
            if (!diagnostics.isEmpty()) {
                diagTable.getItems().add(Map.of("name", rt.name(), "count", diagnostics.size()));
            }
        });
        caseTable.sort();
        diagTable.sort();
    }

    public void onCaseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            //your code for double click handling here
            SupportCase selectedItem = caseTable.getSelectionModel().getSelectedItem();
            if (selectedItem != null && mouseEvent.getTarget() != null && mouseEvent.getTarget().toString().contains(selectedItem.toString())) {
                editCase(selectedItem);
                mouseEvent.consume();
            }
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            SupportCase selectedItem = caseTable.getSelectionModel().getSelectedItem();
            if (selectedItem != null && mouseEvent.getTarget() != null && mouseEvent.getTarget().toString().contains(selectedItem.toString())) {
                caseTable.setContextMenu(itemContextMenu);
            } else {
                caseTable.setContextMenu(emptyContextMenu);
            }
            mouseEvent.consume();
        }
    }

    public void onCaseKeyPressed(KeyEvent keyEvent) {
        SupportCase selectedItem = caseTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null)
            return;
        if ("N".equals(keyEvent.getCode().getChar()) && keyEvent.isControlDown()) {
            newCase();
            keyEvent.consume();
        } else if (keyEvent.getCode() == KeyCode.DELETE) {
            deleteCase(selectedItem);
            keyEvent.consume();
        } else if (keyEvent.getCode() == KeyCode.ENTER) {
            editCase(selectedItem);
            keyEvent.consume();
        }
    }

    private void editCase(SupportCase selectedItem) {
        //AlertHelper.show(INFORMATION, "Item " + selectedItem + " launched");
        if (selectedItem != null)
            MAIN.editSupportCase(selectedItem);
    }

    private void deleteCase(SupportCase selectedItem) {
        if (selectedItem != null) {
            CAT.deleteSupportCase(selectedItem);
            caseTable.getItems().remove(selectedItem);
        }
    }

    private void newCase() {
        SupportCase selectedItem = CAT.createSupportCases("TORNADO-xxx");
        editCase(selectedItem);
    }
}
