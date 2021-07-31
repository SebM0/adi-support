package com.axway.adi.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.CheckedConsumer;
import com.axway.adi.tools.util.RedoLogHasher;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import static javafx.scene.control.Alert.AlertType.WARNING;

public class RedoLogController {
    private static class Statistics {
        private int transactionCount = 0;
        private final Map<String, Integer> errors = new HashMap<>();
        private final Map<String, Object> stats = new HashMap<>();
        private Long minSize = null;
        private Long maxSize = null;

        void addError(String code) {
            errors.compute(code, (key, count) -> count != null ? ++count : 1);
        }

        void addStat(String code, Object value) {
            stats.put(code, value);
        }

        void increaseTransaction() {
            transactionCount++;
        }

        void addSize(long size) {
            minSize = minSize != null ? Math.min(minSize, size) : size;
            maxSize = maxSize != null ? Math.max(maxSize, size) : size;
        }

        void clear() {
            transactionCount = 0;
            errors.clear();
            stats.clear();
        }

        public void aggregate() {
            if (minSize != null)
                addStat("Minimum transaction size", minSize);
            if (maxSize != null)
                addStat("Maximum transaction size", maxSize);
        }
    }
    // Control bindings
    public TextField redoFile;
    public Label transactionCount;
    public Label errorCount;
    public TableView<Map<String,Object>> errorTable;
    public Button browseButton;
    public Button runButton;

    private boolean inited = false;
    private final Stage parentStage;
    private final Properties properties;
    private final Statistics statistics = new Statistics();
    private static final String MRU = "LastFile";
    private static final String PROPERTIES = "redolog.properties";
    private static final int REDOLOG_HEADER_SIZE = 12;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public RedoLogController(Stage parentStage) {
        this.parentStage = parentStage;
        properties = loadProperties();
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
            AlertHelper.show(WARNING, "Missing Redo Log file");
        }

        updateProperties(redoFile.getText());
        startExecution();

        CheckedConsumer<ObjectInputStream, IOException> transactionReader;
        if (redoLogFile.getName().startsWith("DB_"))
            transactionReader = this::readDatabaseTransaction;
        else if (redoLogFile.getName().startsWith("WF_"))
            transactionReader = this::readWorkflowTransaction;
        else
            transactionReader = null;

        new Thread(() -> readRedoLog(redoLogFile, transactionReader)).start();
    }

    private void startExecution() {
        if (!inited) {
            inited = true;
            TableColumn<Map<String, Object>, Object> valueColumn = (TableColumn<Map<String, Object>, Object>)errorTable.getColumns().get(1);
            valueColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item.toString());
                        setTooltip(new Tooltip("Live is short, make most of it..!"));
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
        transactionCount.setText("" + statistics.transactionCount);
        errorCount.setText("" + (int) statistics.errors.values().stream().mapToDouble(u -> u).sum());
        statistics.errors.forEach(this::addErrorTableItem);
        statistics.stats.forEach(this::addErrorTableItem);
        browseButton.setDisable(false);
        runButton.setDisable(false);
    }

    private void addErrorTableItem(String key, Object count) {
        errorTable.getItems().add(Map.of("error", key, "count", count));
    }

    private void readRedoLog(File redoLogFile, CheckedConsumer<ObjectInputStream,IOException> transactionReader) {

        try (FileInputStream reader = new FileInputStream(redoLogFile)) {
            FileChannel channel = reader.getChannel();
            final ByteBuffer headerBuffer = ByteBuffer.allocate(REDOLOG_HEADER_SIZE);

            while (reader.available() > 0) {
                // Read header
                headerBuffer.clear();
                if (channel.read(headerBuffer) < REDOLOG_HEADER_SIZE) {
                    statistics.addError("Incomplete header");
                    break;
                }
                statistics.increaseTransaction();
                // Decode header
                headerBuffer.flip();
                long remaining = headerBuffer.getLong();
                statistics.addSize(remaining);
                int hash = headerBuffer.getInt();
                if (remaining > 0) {
                    // Read transaction bytes
                    ByteBuffer transactionBuffer = ByteBuffer.allocate((int) remaining);
                    if (channel.read(transactionBuffer) < remaining) {
                        statistics.addError("Incomplete transaction");
                        break;
                    }
                    // Compute hash
                    transactionBuffer.flip();
                    RedoLogHasher hasher = new RedoLogHasher(0);
                    hasher.putBytes(transactionBuffer);
                    if (hasher.hash() != hash) {
                        statistics.addError("Invalid transaction hash");
                    }
                    // Read transaction
                    if (transactionReader != null) {
                        transactionBuffer.flip();
                        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(transactionBuffer.array()))) {
                            transactionReader.accept(ois);
                        }
                    }
                } else if (remaining == 0) {
                    statistics.addError("Empty transaction");
                    // continue anyway //break;
                } else {
                    statistics.addError("Negative transaction");
                    break;
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to read Redo Log file: " + redoLogFile + "\n" + ioException.getMessage());
        } finally {
            Platform.runLater(this::stopExecution);
        }
    }

    private void readDatabaseTransaction(ObjectInputStream ois) throws IOException {
        long transactionTime = ois.readLong();
        boolean readCheckpointCommand = ois.readBoolean();

        statistics.stats.computeIfAbsent("First TT", k -> transactionTime);
        Long previousTT = (Long)statistics.stats.get("Last TT");
        if (previousTT != null && previousTT >= transactionTime)
            statistics.addError("Non incremental TT");
        statistics.addStat("Last TT", transactionTime);
        statistics.stats.computeIfAbsent("Checkpoint commands", k -> 0);
        if (readCheckpointCommand)
            statistics.stats.computeIfPresent("Checkpoint commands", (k, v) -> (Integer) v + 1);
    }

    private void readWorkflowTransaction(ObjectInputStream ois) {

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
