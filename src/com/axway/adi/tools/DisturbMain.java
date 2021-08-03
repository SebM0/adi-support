package com.axway.adi.tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.util.DiagnosticCatalog;
import com.axway.adi.tools.util.DiagnosticPersistence;
import com.axway.adi.tools.util.db.SupportCase;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class DisturbMain extends Application {

    public static DisturbMain MAIN;

    public final DiagnosticPersistence DB = new DiagnosticPersistence();
    public final DiagnosticCatalog CAT = new DiagnosticCatalog();

    private static final String PROPERTIES_PATH = "disturb.properties";
    private static final String ROOT = "LocalRoot";
    private final Properties properties = new Properties();

    private Stage stage;
    private Scene welcomeScene;
    private DisturbWelcomeController welcomeController;
    private Scene caseScene;
    private DisturbCaseController caseController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        MAIN = this;
        stage = primaryStage;

        loadProperties();

        primaryStage.setTitle("DISTURB (Decision Insight Support Tool U Require, Baby)");

        loadWelcomeScene();
        loadCaseScene();
        primaryStage.setScene(welcomeScene);

        DB.connect();
        CAT.load();

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
        welcomeController.loadData();
        primaryStage.show();
    }

    void editSupportCase(SupportCase supportCase) {
        caseController.setSupportCase(supportCase);
        stage.setScene(caseScene);
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

    public static void main(String[] args) {
        launch(args);
    }
}
