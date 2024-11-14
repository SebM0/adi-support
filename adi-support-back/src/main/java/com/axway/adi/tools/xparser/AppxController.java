package com.axway.adi.tools.xparser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.FileUtils;
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
import javafx.stage.DirectoryChooser;

import static javafx.scene.control.Alert.AlertType.WARNING;

public class AppxController {
    private static class Count {
        int aloneCount = 0;
        int totalCount = 0;

        Count add(boolean alone) {
            totalCount++;
            if (alone)
                aloneCount++;
            return this;
        }

        @Override
        public String toString() {
            return "alone=" + aloneCount + " / " + totalCount;
        }
    }
    private static class Statistics {
        private final Map<Pagelet, Integer> pageletCount = new HashMap<>();
        private final Map<Mashlet, Count> mashletCount = new HashMap<>();
        private final Map<Mashlet, Map<Pagelet, Integer>> stats = new HashMap<>();

        void addStat(Pagelet pagelet, List<Mashlet> mashlets) {
            pageletCount.compute(pagelet, (k, v) -> v != null ? v+1 : 1);
            for (Mashlet mashlet : mashlets) {
                if (mashlet.isChart()) {
                    mashletCount.compute(mashlet, (k, v) -> v != null ? v.add(mashlets.size() > 1) : new Count().add(mashlets.size() > 1));
                    stats.computeIfAbsent(mashlet, k -> new HashMap<>()).compute(pagelet, (k, v) -> v != null ? v + 1 : 1);
                }
            }
        }

        void clear() {
            mashletCount.clear();
            pageletCount.clear();
            stats.clear();
        }
    }
    // Control bindings
    public TextField appxDirectory;
    public Label transactionCount;
    public Label errorCount;
    public TableView<Map<String,Object>> errorTable;
    public Button browseButton;
    public Button runButton;

    private boolean inited = false;
    private final Properties properties;
    //private final LinkedHashMap<String, Statistics> statistics = new LinkedHashMap<>();
    private final Statistics statistics = new Statistics();
    private static final String MRU = "LastFile";
    private static final String PROPERTIES = "appx.properties";

    public AppxController() {
        properties = loadProperties();
    }

    public void bindControls() {
        if (properties.containsKey(MRU)) {
            appxDirectory.setText(properties.getProperty(MRU));
        }
    }

    @FXML
    public void onBrowse(Event e) {
        e.consume();
        String value = appxDirectory.getText();
        if (value.isBlank() && properties.containsKey(MRU)){
            value = properties.getProperty(MRU);
        }
        DirectoryChooser fileChooser = new DirectoryChooser();
        if (!value.isBlank()) {
            File initialDirectory = new File(value);
            if (initialDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(initialDirectory);
            }
        }
        File file = fileChooser.showDialog(null);
        if (file != null) {
            appxDirectory.setText(file.getPath());
            updateProperties(file.getPath());
        }
    }

    @FXML
    public void onRun(Event e) {
        e.consume();
        // Checks
        File appxDirectoryFile = new File(appxDirectory.getText());
        if (appxDirectory.getText().isBlank() || !appxDirectoryFile.exists()) {
            AlertHelper.show(WARNING, "Missing APPX directory");
        }

        updateProperties(appxDirectory.getText());
        startExecution();

        new Thread(() -> parseAppxFiles(appxDirectoryFile)).start();
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
        transactionCount.setText("" + statistics.pageletCount.values().stream().mapToInt(u -> u).sum());
        errorCount.setText("" + statistics.mashletCount.values().stream().mapToInt(u -> u.totalCount).sum());
        statistics.stats.forEach((mashlet, pagelets) -> addErrorTableItem(mashlet.toString(), pagelets.toString()));
        statistics.mashletCount.forEach((mashlet, counter) -> addErrorTableItem(mashlet.toString(), counter.toString()));
        browseButton.setDisable(false);
        runButton.setDisable(false);
    }

    private void addErrorTableItem(String mashlet, String pagelets) {
        errorTable.getItems().add(Map.of("error", mashlet, "count", pagelets));
    }

    private void parseAppxFiles(File appxDirectoryFile) {
        try {
            if (appxDirectoryFile.isDirectory()) {
                try (Stream<Path> list = Files.walk(appxDirectoryFile.toPath())) {

                    list.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".appx")).forEach(this::parseAppxFile);
                }
            } else {
                parseAppxFile(appxDirectoryFile.toPath());
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to scan application directory: " + appxDirectoryFile + "\n" + ioException.getMessage());
        } finally {
            Platform.runLater(this::stopExecution);
        }
    }

    private void parseAppxFile(Path appxFile) {
        System.out.println("Parsing " + appxFile);
        try {
            String deploymentFolder = unzip(appxFile.toAbsolutePath().toString());
            try (Stream<Path> stream = Files.walk(Path.of(deploymentFolder), 3)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().compareToIgnoreCase("format.xml") == 0)
                        .forEach(this::parsePageletFormat);
            }
        } catch (IOException e) {
            AlertHelper.show(WARNING, "Failed to parse application: " + appxFile + "\n" + e.getMessage());
        }
    }

    private void parsePageletFormat(Path appxFile) {
        try {
            // Load file as XML
            FileInputStream fis = new FileInputStream(appxFile.toFile());
            Document document = FileUtils.parseDocument(fis);
            Element xmlRoot = document.getDocumentElement();
            String tag = xmlRoot.getTagName();
            Pagelet pagelet = Pagelet.fromType(tag);
            NodeList childNodes = xmlRoot.getElementsByTagName("format");
            List<Mashlet> mashlets = IntStream.range(0, childNodes.getLength())
                    .mapToObj(childNodes::item)
                    .filter(c -> c.getNodeType() == Node.ELEMENT_NODE)
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast)
                    .map(format -> Mashlet.fromType(format.getAttribute("class")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<Mashlet> chartMashlets = mashlets.stream().filter(Mashlet::isChart).collect(Collectors.toList());
            if (pagelet != null) {
                if (!chartMashlets.isEmpty()) {
                    System.out.println("Found " + mashlets + " in " + pagelet);
                }
                statistics.addStat(pagelet, mashlets);
            }
        } catch (IOException e) {
            AlertHelper.show(WARNING, "Failed to parse application: " + appxFile + "\n" + e.getMessage());
        }
    }

    private String unzip(String localPath) throws IOException {
        String deploymentFolder = FileUtils.getDeploymentFolder(localPath);
        unzip(localPath, deploymentFolder);

        // 2nd pass if extracted file is a "tar"
        AtomicReference<IOException> excepCollector = new AtomicReference<>();
        try (Stream<Path> stream = Files.walk(Path.of(deploymentFolder), Integer.MAX_VALUE)) {
            stream.filter(Files::isRegularFile).filter(file -> FileUtils.isArchive(file.getFileName())).forEach(subPath -> {
                try {
                    unzip(subPath.toString(), deploymentFolder);
                    Files.delete(subPath);
                } catch (IOException e) {
                    excepCollector.set(e);
                }
            });
        }
        if (excepCollector.get() != null) {
            throw excepCollector.get();
        }
        return deploymentFolder;
    }

    private void unzip(String localPath, String deploymentFolder) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("7z x \"" + localPath + "\" -bd -y -o\"" + deploymentFolder + "\"").getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
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
}
