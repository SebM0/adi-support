package com.axway.adi.tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.operatiions.DownloadOperation;
import com.axway.adi.tools.operatiions.OperationExecutor;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.DbConstants.ResourceType;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.SupportCase;
import com.axway.adi.tools.util.db.SupportCaseResource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import static com.axway.adi.tools.DisturbMain.MAIN;
import static javafx.scene.control.Alert.AlertType.ERROR;

public class DisturbCaseController extends AbstractController {
    public TextField supportCaseId;
    public TextField remotePath;
    public TextField localPath;
    public TextField summary;
    public TextField releaseName;
    public TextField customerName;
    public Button runButton;
    public Label lastRunLabel;
    public ProgressBar progress;
    public Label progressLabel;
    public TableView<SupportCaseResource> resourceTable;
    public TableView<DiagnosticResult> resultTable;

    private SupportCase supportCase;
    private OperationExecutor executor;
    private static final String ROOT = "LocalRoot";

    private static final class ResourceTypeProperty extends SimpleStringProperty {
        ResourceTypeProperty(SupportCaseResource item) {
            super(null, item.name);
            setValue(ResourceType.values()[item.type].name());
            addListener((observable, oldValue, newValue) -> item.type = ResourceType.valueOf(newValue).ordinal());
        }

    }

    @Override
    void bindControls(Stage parentStage) {
        super.bindControls(parentStage);
        // Bind resourceTable
        {
            resourceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ObservableList<TableColumn<SupportCaseResource, ?>> columns = resourceTable.getColumns();
            TableColumn<SupportCaseResource, String> nameColumn = (TableColumn<SupportCaseResource, String>) columns.get(0);
            nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().toString()));
            TableColumn<SupportCaseResource, String> typeColumn = (TableColumn<SupportCaseResource, String>) columns.get(1);
            typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(Arrays.stream(ResourceType.values()).map(Object::toString).toArray(String[]::new)));
            typeColumn.setCellValueFactory(cellData -> new ResourceTypeProperty(cellData.getValue()));
        }
        // Bind resultTable
        {
            ObservableList<TableColumn<DiagnosticResult, ?>> columns = resultTable.getColumns();
            TableColumn<DiagnosticResult, String> nameColumn = (TableColumn<DiagnosticResult, String>) columns.get(0);
            nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().toString()));
        }
    }

    void setSupportCase(SupportCase supportCase) {
        // Init info
        this.supportCase = supportCase;
        supportCaseId.setText(supportCase.id);
        releaseName.setText(supportCase.release);
        customerName.setText(supportCase.customer);
        summary.setText(supportCase.summary);

        // Reset other fields
        lastRunLabel.setText("Last run: -");
        ObservableList<SupportCaseResource> resourceTableItems = resourceTable.getItems();
        resourceTableItems.clear();
        supportCase.getItems().stream().filter(i -> !i.ignored).forEach(resourceTableItems::add);
    }

    public void onLoadJira(ActionEvent actionEvent) {
        String caseId = supportCaseId.getText();
        // Browse cases
        if (caseId.trim().isEmpty()) {
            //TODO browse
        }
        String remote = "https://jira.axway.com/rest/api/2/issue/" + caseId;
        try (Reader reader = new InputStreamReader(Runtime.getRuntime().exec("curl --user rd-tnd-viewer:axwaydi2017 --silent " + remote + "?expand=fields").getInputStream())) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject root = element.getAsJsonObject();
            JsonObject fields = root.getAsJsonObject("fields");
            if (fields == null) {
                AlertHelper.show(ERROR, "Cannot read issue");
                return;
            }
            customerName.setText(fields.getAsJsonArray("customfield_11830").get(0).getAsString());
            releaseName.setText(fields.getAsJsonArray("versions").get(0).getAsJsonObject().getAsJsonPrimitive("name").getAsString());
            summary.setText(fields.getAsJsonPrimitive("summary").getAsString());
            JsonArray atts = fields.getAsJsonArray("attachment");
            atts.forEach(att -> {
                JsonObject attachment = att.getAsJsonObject();
                if (!attachment.getAsJsonPrimitive("mimeType").getAsString().startsWith("image")) {
                    String name = attachment.getAsJsonPrimitive("filename").getAsString();
                    if (supportCase.getItem(name) == null) {
                        addResource(name, attachment.getAsJsonPrimitive("content").getAsString());
                    }
                }
            });
            remotePath.setText(remote);
            supportCase.setRemotePath(remote);
        } catch (IOException e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
    }

    private void addResource(String name, String remotePath) {
        SupportCaseResource res = new SupportCaseResource();
        res.name = name;
        res.remote_path = remotePath;
        ResourceType type = ResourceType.Unknown;
        String testName = name.toLowerCase();
        if (testName.contains("support") && testName.contains("archive")) {
            type = ResourceType.SupportArchive;
        } else if (testName.contains("thread") && testName.contains("dump")) {
            type = ResourceType.ThreadDump;
        } else if (testName.contains(".log")) {
            type = ResourceType.Log;
        } else if (testName.contains(".appx")) {
            type = ResourceType.Appx;
        }
        res.type = type.ordinal();
        resourceTable.getItems().add(res);
        supportCase.addItem(res);
    }

    public void onLoadDisk(ActionEvent actionEvent) {
        String caseId = supportCaseId.getText();
        // search local path
        Path root = Path.of(MAIN.getProperty(ROOT));
        try {
            if (Files.exists(root)) {
                Files.createDirectories(root);
            }
            if (Files.isDirectory(root)) {
                Path resDir = Path.of(root.toString(), caseId);
                Files.createDirectories(resDir);
                String local = resDir.toString();
                localPath.setText(local);
                supportCase.setLocalPath(local);
            }
        } catch (IOException e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
    }

    public void onResourceKeyPressed(KeyEvent keyEvent) {
        if ("A".equals(keyEvent.getCode().getChar()) && keyEvent.isControlDown()) {
            resourceTable.getSelectionModel().selectAll();
            keyEvent.consume();
        } else if (keyEvent.getCode().getCode() == 127) { //DELETE
            SupportCaseResource[] supportCaseResources = resourceTable.getSelectionModel().getSelectedItems().toArray(new SupportCaseResource[0]);
            Arrays.stream(supportCaseResources).forEach(item -> item.ignored = true);
            resourceTable.getItems().removeAll(supportCaseResources);
        }
    }

    public void onRun(ActionEvent actionEvent) {
        if (resourceTable.getItems().isEmpty()) {
            AlertHelper.show(ERROR, "No resource to analyze");
            return;
        }
        // kill if running
        if (executor != null) {
            executor.kill();
            executor = null;
            return;
        }
        progress.setProgress(0);
        executor = new OperationExecutor();
        resourceTable.getItems().forEach(this::submit);
        // bind progress properties
        progress.progressProperty().unbind();
        progress.progressProperty().bind(executor.progressProperty());
        progressLabel.textProperty().unbind();
        progressLabel.textProperty().bind(executor.messageProperty());
        // When completed tasks
        executor.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, //
                                 t -> {
                                     progressLabel.textProperty().unbind();
                                     progressLabel.setText("Done");
                                 });

        // Start the Task.
        new Thread(executor).start();
    }

    private void submit(SupportCaseResource res) {
        executor.addOperation(new DownloadOperation(res));
        String testName = res.name.toLowerCase();
        if (testName.endsWith(".zip") || testName.endsWith(".gz") || testName.endsWith(".tar")) {
            // deploy
        }
    }

    public void onSave(ActionEvent actionEvent) {
        MAIN.DB.insert(supportCase);
        supportCase.getItems().forEach(item -> MAIN.DB.insert(item));
        MAIN.welcome();
    }
}
