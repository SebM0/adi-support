package com.axway.adi.tools.disturb;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.operatiions.DeployOperation;
import com.axway.adi.tools.disturb.parsers.ApplicationParser;
import com.axway.adi.tools.disturb.parsers.FileListParser;
import com.axway.adi.tools.disturb.parsers.LogParser;
import com.axway.adi.tools.disturb.parsers.Parser;
import com.axway.adi.tools.disturb.parsers.ThreadDumpParser;
import com.axway.adi.tools.util.AlertHelper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import static javafx.scene.control.Alert.AlertType.WARNING;

public class SingleFileParserController implements Initializable {
    private static class Statistics {
        final List<DiagnosticResult> results = new ArrayList<>();
        int transactionCount;

        void clear() {
            results.clear();
            transactionCount = 0;
        }
    }

    // Control bindings
    public TextField redoFile;
    public Label transactionCount;
    public Label errorCount;
    public TableView<Map<String,Object>> errorTable;
    public Button browseButton;
    public Button runButton;
    public CheckBox hideIdleThreads;

    private boolean inited = false;
    private Stage parentStage;
    private Properties properties;
    private final Statistics statistics = new Statistics();
    private static final String MRU = "LastFile";
    private static final String PROPERTIES = "threaddump.properties";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        properties = loadProperties();
    }

    void bindControls(Stage parentStage) {
        this.parentStage = parentStage;
        redoFile.setText(properties.getProperty(MRU));
    }

    @FXML
    public void onBrowse(Event e) {
        FileChooser fileChooser = new FileChooser();
        if (!redoFile.getText().isBlank()) {
            fileChooser.setInitialDirectory(new File(redoFile.getText()).getParentFile());
        } else if (properties.containsKey(MRU)){
            fileChooser.setInitialDirectory(new File(properties.getProperty(MRU)).getParentFile());
        }
        File file = fileChooser.showOpenDialog(parentStage);
        if (file != null) {
            redoFile.setText(file.getPath());
            updateProperties(file.getPath());
        }
        e.consume();
    }

    @FXML
    public void onRun(Event e) {
        // Checks
        File redoLogFile = new File(redoFile.getText());
        if (redoFile.getText().isBlank() || !redoLogFile.isFile()) {
            AlertHelper.show(WARNING, "Missing file");
        }

        updateProperties(redoFile.getText());
        startExecution();

        new Thread(() -> analyzeFile(redoLogFile)).start();
        e.consume();
    }

    @SuppressWarnings("unchecked")
    private void startExecution() {
        if (!inited) {
            inited = true;
            ObservableList<TableColumn<Map<String, Object>, ?>> columns = errorTable.getColumns();
            TableColumn<Map<String, Object>, Object> nameColumn = (TableColumn<Map<String, Object>, Object>) columns.get(0);
            nameColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item instanceof DiagnosticResult) {
                        DiagnosticResult result = (DiagnosticResult) item;
                        setText(result.getSpecName());
                        setTooltip(new Tooltip(result.notes));
                    }
                }
            });
        }
        // Clear view
        statistics.clear();
        transactionCount.setText("-");
        errorCount.setText("-");
        errorTable.getItems().clear();
        browseButton.setDisable(true);
        runButton.setDisable(true);
    }

    private void stopExecution() {
        transactionCount.setText("" + statistics.transactionCount);
        errorCount.setText("" + statistics.results.size());
        browseButton.setDisable(false);
        runButton.setDisable(false);
    }

    public void onHideIdle(ActionEvent actionEvent) {
        //errorTable.getItems().clear();
        //statistics.dumps.forEach(this::addErrorTableItem);
        actionEvent.consume();
    }

    private void addErrorTableItem(DiagnosticResult key) {
        //boolean hideIdle = hideIdleThreads.isSelected();
        statistics.results.add(key);
        Platform.runLater(() -> {
            errorTable.getItems().add(Map.of("spec", key, "diag", key.notes));
            errorTable.refresh();
        });
    }

    private void analyzeFile(File file) {
        Parser parser = createParser(file);

        try  {
            parser.parse(this::addErrorTableItem);
            statistics.transactionCount = parser.getSize();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to read file: " + file + "\n" + ioException.getMessage());
        } finally {
            Platform.runLater(this::stopExecution);
        }
    }

    private static Parser createParser(File file) {
        SupportCaseResource res = new SupportCaseResource();
        res.local_path = file.getAbsolutePath();
        if (file.getName().toLowerCase().contains(".log")) {
            return new LogParser(res);
        }
        if (file.getPath().toLowerCase().contains("file-list")) {
            return new FileListParser(res);
        }
        if (file.getPath().toLowerCase().contains("application.xml")) {
            return new ApplicationParser(res);
        }
        if (file.getPath().toLowerCase().contains(".appx")) {
            DeployOperation deploy = new DeployOperation(res);
            deploy.run();
            return new ApplicationParser(res);
        }
        return new ThreadDumpParser(res);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream propertiesStream = Files.newInputStream(Path.of(PROPERTIES))) {
            properties.load(propertiesStream);
        } catch (NoSuchFileException e) {
            // skip
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private void updateProperties(String lastFile) {
        properties.setProperty(MRU, lastFile);
        try (OutputStream propertiesStream = Files.newOutputStream(Path.of(PROPERTIES))) {
            properties.store(propertiesStream, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String decodeTT(long tt) {
        long timestamp = tt >> 20;
        return LocalDate.ofEpochDay(timestamp).format(FORMATTER);
    }
}
