package com.axway.adi.tools;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

public class ThreadDumpController implements Initializable {
    private static class Dump {
        private static final String SYSTAR = "com.systar.";
        private static final Set<String> UTILITIES = Set.of("tau", "gluon", "boson", "photon");
        String header;
        String name;
        String status;
        boolean idle = false;
        final List<String> errors = new ArrayList<>();
        final List<String> stack = new ArrayList<>();
        final LinkedList<String> traversedComponents = new LinkedList<>();

        public Dump(String header) {
            this.header = header;
            int pos = header.indexOf('\"', 1);
            if (pos > 0)
                name = header.substring(1, pos);
        }

        String getErrors() {
            return errors.isEmpty() ? "" : String.join("\n", errors);
        }

        String getStackTrace() {
            return String.join("\n", stack);
        }

        public void aggregate() {
            String lock = "";
            for (String trace : stack) {
                if (trace.startsWith("at ")) {
                    trace = trace.substring(3);
                    String component = getComponent(trace);
                    if (component != null) {
                        if (traversedComponents.isEmpty() || !component.equals(traversedComponents.getLast())) {
                            traversedComponents.add(component);
                        }
                        if ("calcium".equals(component) && trace.contains("awaitVectorClockProgress")) {
                            errors.add("Absorption blocked");
                        }
                    }
                } else if (trace.startsWith("- ")) {
                    lock = trace.substring(2);
                }
            }
            idle = status.contains("WAITING") && (traversedComponents.isEmpty() //
                    || (traversedComponents.size() == 1 && "calcium".equals(traversedComponents.getLast()) && name.contains("-asynchronousEventInterceptorHandlers.")) // asynchronous event threads idle
            );
        }

        private String getComponent(String trace) {
            int pos = trace.indexOf(SYSTAR);
            if (pos == -1)
                return null;
            String component = trace.substring(pos + SYSTAR.length());
            pos = component.indexOf('.');
            if (pos > 0) {
                String subComponent = component.substring(pos + 1);
                component = component.substring(0, pos);
                // ignore utility components
                if (UTILITIES.contains(component))
                    return null;
                pos = subComponent.indexOf('.');
                if (pos > 0) {
                    subComponent = subComponent.substring(0, pos);
                    if (!"impl".equals(subComponent) && Character.isLowerCase(subComponent.charAt(0))) {
                        component += "." + subComponent;
                    }
                }
            }
            return component;
        }
    }
    private static class Statistics {
        private static final String STATUS_HEADER = "java.lang.Thread.State:";
        private final List<Dump> dumps = new ArrayList<>();
        Dump current = null;

        void addThread(String header) {
            current = new Dump(header);
            dumps.add(current);
        }

        void addError(String code) {
            current.errors.add(code);
        }

        void addStack(String code) {
            if (code.startsWith(STATUS_HEADER)) {
                current.status = code.substring(STATUS_HEADER.length()).trim();
            } else {
                current.stack.add(code);
            }
        }

        void clear() {
            dumps.clear();
            current = null;
        }

        public void aggregate() {
            dumps.forEach(Dump::aggregate);
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

        new Thread(() -> readThreadDump(redoLogFile)).start();
    }

    private void startExecution() {
        if (!inited) {
            inited = true;
            ObservableList<TableColumn<Map<String, Object>, ?>> columns = errorTable.getColumns();
            TableColumn<Map<String, Object>, Object> nameColumn = (TableColumn<Map<String, Object>, Object>) columns.get(0);
            nameColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item instanceof Dump) {
                        Dump dump = (Dump) item;
                        setText(dump.name);
                        setTooltip(new Tooltip(dump.getStackTrace()));
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
        statistics.aggregate();
        transactionCount.setText("" + statistics.dumps.size());
        errorCount.setText("" + statistics.dumps.stream().flatMap(d -> d.errors.stream()).count());
        statistics.dumps.forEach(this::addErrorTableItem);
        browseButton.setDisable(false);
        runButton.setDisable(false);
    }

    public void onHideIdle(ActionEvent actionEvent) {
        errorTable.getItems().clear();
        statistics.dumps.forEach(this::addErrorTableItem);
        actionEvent.consume();
    }

    private void addErrorTableItem(Dump key) {
        boolean hideIdle = hideIdleThreads.isSelected();
        if (!hideIdle || !key.idle || !key.errors.isEmpty()) {
            errorTable.getItems().add(Map.of("thread", key, "status", key.status, "idle", key.idle, "error", key.getErrors()));
        }
    }

    private void readThreadDump(File redoLogFile) {

        try (BufferedReader reader = new BufferedReader(new FileReader(redoLogFile))) {
            String line;
            boolean header = true;
            boolean blankLine = false;
            boolean inThread = false;
            while ((line = reader.readLine()) != null) {
                boolean previousLineBlank = blankLine;
                blankLine = line.isBlank();
                if (blankLine) {
                    inThread = false;
                    continue;
                }
                line = line.trim();
                // Read header
                if (header) {
                    if (line.startsWith("\"")) {
                        header = false;
                    }
                }
                if (previousLineBlank && line.startsWith("\"")) {
                    inThread = true;
                    statistics.addThread(line);
                } else if (inThread) {
                    statistics.addStack(line);
                }
                //statistics.addError("Incomplete header");
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to read Redo Log file: " + redoLogFile + "\n" + ioException.getMessage());
        } finally {
            Platform.runLater(this::stopExecution);
        }
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
