package com.axway.adi.tools;

import java.net.URL;
import java.util.*;
import javafx.fxml.Initializable;
import javafx.stage.Stage;

public abstract class AbstractController implements Initializable {
    protected Stage parentStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // override if needed
    }

    protected void bindControls(Stage parentStage) {
        this.parentStage = parentStage;
    }
}
