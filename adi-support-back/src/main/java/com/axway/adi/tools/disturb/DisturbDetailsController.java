package com.axway.adi.tools.disturb;

import java.util.stream.*;
import com.axway.adi.tools.AbstractController;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticResultItem;
import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class DisturbDetailsController extends AbstractController {
    public TextField name;
    public TextField description;
    public TextField remediation;
    public TextArea notes;
    public TextArea details;

    public void setResult(DiagnosticResult diagnosticResult) {
        name.setText(diagnosticResult.getSpecName());
        description.setText(diagnosticResult.getSpec().description);
        remediation.setText(diagnosticResult.getSpec().remediation);
        notes.setText(diagnosticResult.notes);
        details.setText(diagnosticResult.getItems().stream().map(DiagnosticResultItem::toString).collect(Collectors.joining("\n")));
    }

    public void onClose(ActionEvent actionEvent) {
        DisturbMain.MAIN.editSupportCase(null);
        actionEvent.consume();
    }
}
