package com.axway.adi.tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.operatiions.DeployOperation;
import com.axway.adi.tools.operatiions.DiscoverOperation;
import com.axway.adi.tools.operatiions.DownloadOperation;
import com.axway.adi.tools.operatiions.OperationExecutor;
import com.axway.adi.tools.operatiions.ScanOperation;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.ImageTableCell;
import com.axway.adi.tools.util.db.DbConstants.Level;
import com.axway.adi.tools.util.db.DbConstants.ResourceType;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.SupportCase;
import com.axway.adi.tools.util.db.SupportCaseResource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
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
import static com.axway.adi.tools.util.DiagnosticPersistence.DB;
import static com.axway.adi.tools.util.FileUtils.getDeploymentFolder;
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

    private static final class ResourceTypeProperty extends SimpleStringProperty {
        ResourceTypeProperty(SupportCaseResource item) {
            super(null, item.name);
            setValue(ResourceType.values()[item.type].name());
            addListener((observable, oldValue, newValue) -> item.type = ResourceType.valueOf(newValue).ordinal());
        }

    }

    @SuppressWarnings("unchecked")
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
            TableColumn<DiagnosticResult, Number> LevelColumn = (TableColumn<DiagnosticResult, Number>) columns.get(0);
            LevelColumn.setCellFactory(column -> new ImageTableCell<>(Arrays.stream(Level.values()).map(Object::toString).toArray(String[]::new)));
            LevelColumn.setCellValueFactory(cellData -> new ReadOnlyIntegerWrapper(cellData.getValue().getLevel()));
            TableColumn<DiagnosticResult, String> resourceColumn = (TableColumn<DiagnosticResult, String>) columns.get(1);
            resourceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getParentResource()));
            TableColumn<DiagnosticResult, String> diagColumn = (TableColumn<DiagnosticResult, String>) columns.get(2);
            diagColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSpecName()));
            TableColumn<DiagnosticResult, String> resultColumn = (TableColumn<DiagnosticResult, String>) columns.get(3);
            resultColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().toString()));
        }
    }

    void setSupportCase(SupportCase supportCase) {
        // Init info
        this.supportCase = supportCase;
        supportCaseId.setText(supportCase.id);
        releaseName.setText(supportCase.release);
        customerName.setText(supportCase.customer);
        summary.setText(supportCase.summary);
        remotePath.setText(supportCase.remote_path);
        localPath.setText(supportCase.getLocalPath());

        // Reset other fields
        lastRunLabel.setText("Last run: -");
        ObservableList<SupportCaseResource> resourceTableItems = resourceTable.getItems();
        resourceTableItems.clear();
        supportCase.getResources().stream().filter(i -> !i.ignored).forEach(resourceTableItems::add);
    }

    public void onLoadJira(ActionEvent actionEvent) {
        String caseId = supportCaseId.getText().trim();
        supportCase.id = caseId;
        // Browse cases
        if (caseId.isEmpty()) {
            //TODO browse open tornado cases
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
            JsonElement child = fields.get("customfield_11830");
            if (child instanceof JsonArray) {
                customerName.setText(((JsonArray)child).get(0).getAsString());
            }
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
            supportCase.remote_path = remote;
            buildDefaultLocalDirectory();
        } catch (IOException e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
        actionEvent.consume();
    }

    private SupportCaseResource addResource(String name, String remotePath) {
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
        return res;
    }

    public void buildDefaultLocalDirectory() {
        try {
            if (supportCase.local_path == null || supportCase.local_path.isEmpty()) {
                String caseId = supportCaseId.getText().trim();
                supportCase.id = caseId;
                // search local path
                Path root = Path.of(MAIN.getRootDirectory());
                if (Files.exists(root)) {
                    Files.createDirectories(root);
                }
                if (Files.isDirectory(root)) {
                    supportCase.local_path = caseId;
                    Path resDir = Path.of(root.toString(), caseId);
                    Files.createDirectories(resDir);
                    localPath.setText(resDir.toString());
                }
            }
        } catch (IOException e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
    }

    public void onLoadDisk(ActionEvent actionEvent) {
        // Connect to local path
        buildDefaultLocalDirectory();

        // Browse
        Path caseRoot = Path.of(supportCase.getLocalPath());
        Set<Path> excludedDirectories = new HashSet<>();
        try {
            // Browse files first
            Files.list(caseRoot) //
                    .filter(Files::isRegularFile) //
                    .forEach(sub -> {
                        SupportCaseResource res = addResource(sub.getFileName().toString(), null);
                        res.local_path = sub.toString();
                        try {
                            String deploymentFolder = getDeploymentFolder(res.local_path);
                            Path deploymentPath = Path.of(deploymentFolder);
                            if (deploymentFolder != null && !deploymentFolder.isEmpty() && Files.isDirectory(deploymentPath)) {
                                res.local_ex_path = deploymentFolder;
                                excludedDirectories.add(deploymentPath);
                            }
                        } catch (FileNotFoundException e) {
                            //skip
                        }
                    });
            // Browse directories
            Files.list(caseRoot) //
                    .filter(sub -> Files.isDirectory(sub) && !excludedDirectories.contains(sub)) //
                    .forEach(sub -> {
                        SupportCaseResource res = addResource(sub.getFileName().toString(), null);
                        res.local_path = sub.toString();
                    });
        } catch (IOException e) {
            // skip
        }
        actionEvent.consume();
    }

    public void onResourceKeyPressed(KeyEvent keyEvent) {
        if ("A".equals(keyEvent.getCode().getChar()) && keyEvent.isControlDown()) {
            resourceTable.getSelectionModel().selectAll();
            keyEvent.consume();
        } else if (keyEvent.getCode().getCode() == 127) { //DELETE
            SupportCaseResource[] supportCaseResources = resourceTable.getSelectionModel().getSelectedItems().toArray(new SupportCaseResource[0]);
            Arrays.stream(supportCaseResources).forEach(item -> item.ignored = true);
            resourceTable.getItems().removeAll(supportCaseResources);
            keyEvent.consume();
        }
    }

    public void onRun(ActionEvent actionEvent) {
        if (resourceTable.getItems().isEmpty()) {
            AlertHelper.show(ERROR, "No resource to analyze");
            return;
        }
        onRunStarted();
        // Reset
        progress.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progress.setProgress(0);
        progressLabel.setText("Initializing");
        resultTable.getItems().clear();
        // build executor
        executor = new OperationExecutor(this::addResult);
        resourceTable.getItems().forEach(this::submit);
        // bind progress properties
        progress.progressProperty().bind(executor.progressProperty());
        progressLabel.textProperty().bind(executor.messageProperty());
        // When completed tasks
        //executor.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, //
        //                         t -> {
        //                             progressLabel.textProperty().unbind();
        //                             progressLabel.setText("Done");
        //                         });

        // Start the Task.
        executor.start();
        actionEvent.consume();
    }

    private void onRunStarted() {
        runButton.setDisable(true);
    }

    private void onRunTerminated() {
        progress.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        // kill if running
        if (executor != null) {
            executor.kill();
            executor = null;
        }
        progressLabel.setText("Done");
        runButton.setDisable(false);
    }

    private void addResult(DiagnosticResult result) {
        Platform.runLater(() -> {
            if (result != null) {
                resultTable.getItems().add(result);
                resultTable.refresh();
            } else { // end with empty result
                onRunTerminated();
            }
        });
    }

    private void submit(SupportCaseResource res) {
        executor.addOperation(new DownloadOperation(res));
        String testName = res.name.toLowerCase();
        if (testName.endsWith(".zip") || testName.endsWith(".gz") || testName.endsWith(".tar") || res.getResourceType() == ResourceType.Appx) {
            // deploy
            executor.addOperation(new DeployOperation(res));
            // discover
            if (res.getResourceType() == ResourceType.SupportArchive) {
                executor.addOperation(new DiscoverOperation(res));
            }
        }
        executor.addOperation(new ScanOperation(res));
    }

    public void onSave(ActionEvent actionEvent) {
        // flush editor fields
        supportCase.id = supportCaseId.getText();

        if (MAIN.isOnline()) {
            DB.insert(supportCase);
            DB.insert(supportCase.getResources());
        }
        MAIN.welcome();
        actionEvent.consume();
    }
}
