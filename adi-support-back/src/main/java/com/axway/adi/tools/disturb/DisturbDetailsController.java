package com.axway.adi.tools.disturb;

import java.util.stream.*;
import com.axway.adi.tools.AbstractController;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticResultItem;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class DisturbDetailsController extends AbstractController {
    public TextField name;
    public TextField description;
    public TextField remediation;
    public TextArea notes;
    public TextArea details;
    public Button prevButton;
    public Button nextButton;
    private TableView.TableViewSelectionModel<DiagnosticResult> resultsSelectionModel;

    public void setResult(TableView.TableViewSelectionModel<DiagnosticResult> resultsSelectionModel) {
        this.resultsSelectionModel = resultsSelectionModel;
        updateFields();
    }

    private void updateFields() {
        // Text fields
        DiagnosticResult diagnosticResult = resultsSelectionModel.getSelectedItem();
        name.setText(diagnosticResult.getSpecName());
        description.setText(diagnosticResult.getSpec().description);
        remediation.setText(diagnosticResult.getSpec().remediation);
        notes.setText(diagnosticResult.notes);
        details.setText(diagnosticResult.getItems().stream().map(DiagnosticResultItem::toString).collect(Collectors.joining("\n")));
        // Buttons
        int index = resultsSelectionModel.getSelectedIndex();
        int size = resultsSelectionModel.getTableView().getItems().size();
        prevButton.setDisable(index <= 0);
        nextButton.setDisable(index >= size-1);
    }

    public void onClose(ActionEvent actionEvent) {
        DisturbMain.MAIN.editSupportCase(null);
        actionEvent.consume();
    }

    public void onNext(ActionEvent actionEvent) {
        resultsSelectionModel.selectNext();
        updateFields();
        actionEvent.consume();
    }

    public void onPrevious(ActionEvent actionEvent) {
        resultsSelectionModel.selectPrevious();
        updateFields();
        actionEvent.consume();
    }
}
