package com.axway.adi.tools.disturb;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.SupportCase;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static com.axway.adi.tools.disturb.DiagnosticPersistence.DB;

public class DisturbMain extends Application {

    public static DisturbMain MAIN;

    private static final String TITLE = "DISTurb (Decision Insight Support Tool)";
    private static final String PROPERTIES_PATH = "disturb.properties";
    private static final String ROOT = "LocalRoot";
    private final Properties properties = new Properties();

    private Stage stage;
    private Scene welcomeScene;
    private DisturbWelcomeController welcomeController;
    private Scene caseScene;
    private DisturbCaseController caseController;
    private Scene detailScene;
    private DisturbDetailsController detailController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        MAIN = this;
        stage = primaryStage;

        loadProperties();
        loadData();

        primaryStage.setTitle(TITLE + (isOnline() ? "" : " - OFFLINE"));

        loadWelcomeScene();
        loadCaseScene();
        loadDetailScene();
        primaryStage.setScene(welcomeScene);

        welcomeController.loadData();
        primaryStage.show();
    }

    private void loadData() {
        try {
            DB = new DiagnosticPersistence();
            DB.connect();
            debugData();
        } catch (SQLException e) {
            DB = null;
            //AlertHelper.show(Alert.AlertType.WARNING, "Cannot connect to DB\nSwitching to offline mode");
        }
        CAT.load();
    }

    private void debugData() {
/*        DB.executeUpdate("DELETE from \"SUPPORT_CASE\"");
        List<SupportCase> supportCases = DB.select(SupportCase.class);
        if (supportCases.isEmpty()) {
            // create default test case
            SupportCase def = new SupportCase();
            def.id = "TORNADO-2268";
            def.customer = "SOCIETE GENERALE";
            def.application = "BAMIP";
            def.release = "20200803-05";
            def.status = STATUS_IN_PROGRESS;
            DB.insert(def);
            supportCases = DB.select(SupportCase.class);
        }*/
        /*
        DiagnosticSpecification diag = new DiagnosticSpecification();
        diag.id = "TD-0001";
        diag.name = "Counter-performant plan operator";
        diag.setLevel(Error);
        diag.setResourceType(ThreadDump);
        diag.diagnostic = new DiagnosticBuilder().addThreadDumpStackRule("InstantCompositeInstanceIdJoinPhysicalOperator").build();
        diag.description = "";
        diag.remediation = "Migrate to version 20210607 or higher";
        DB.insert(diag);*/
    }

    void editSupportCase(SupportCase supportCase) {
        if (supportCase != null) {
            caseController.setSupportCase(supportCase);
        }
        stage.setScene(caseScene);
    }

    void showDiagnosticDetails(DiagnosticResult diagnosticResult) {
        detailController.setResult(diagnosticResult);
        stage.setScene(detailScene);
    }

    void welcome() {
        CAT.load();
        stage.setScene(welcomeScene);
    }

    private void loadWelcomeScene() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("disturbWelcome.fxml"));
        welcomeScene = new Scene(loader.load(), 1000, 600);
        welcomeController = loader.getController();
        welcomeController.bindControls(stage);
    }

    private void loadCaseScene() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("disturbCase.fxml"));
        caseScene = new Scene(loader.load(), 1000, 800);
        caseController = loader.getController();
        caseController.bindControls(stage);
    }

    private void loadDetailScene() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("disturbDetails.fxml"));
        detailScene = new Scene(loader.load(), 1000, 800);
        detailController = loader.getController();
    }

    private void loadProperties() {
        try (InputStream propertiesStream = Files.newInputStream(Path.of(PROPERTIES_PATH))) {
            properties.load(propertiesStream);
        } catch (NoSuchFileException e) {
            // skip
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateProperties(String key, String value) {
        properties.setProperty(key, value);
        try (OutputStream propertiesStream = Files.newOutputStream(Path.of(PROPERTIES_PATH))) {
            properties.store(propertiesStream, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getRootDirectory() {
        return getProperty(ROOT);
    }

    public boolean isOnline() {
        return DB != null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
