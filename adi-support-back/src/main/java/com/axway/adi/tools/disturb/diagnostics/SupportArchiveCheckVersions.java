package com.axway.adi.tools.disturb.diagnostics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.GlobalContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.SupportArchive;

public class SupportArchiveCheckVersions extends DiagnosticSpecification {

    public SupportArchiveCheckVersions() {
        id = "BUILTIN-SA-0005";
        name = "Version";
        setLevel(DbConstants.Level.Info);
        setResourceType(SupportArchive);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class StatisticsContext extends DiagnosticParseContext<Path> {
        private DiagnosticResult result = null;

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, Path path) {
            try {
                Path aboutPath = path.resolve("about.json");
                if (Files.isRegularFile(aboutPath)) {
                    JsonObject object;
                    try (FileReader fileReader = new FileReader(aboutPath.toFile())) {
                        object = JsonParser.parseReader(fileReader).getAsJsonObject();
                    }
                    if (object != null) {
                        result = buildResult();
                        String productRelease = object.getAsJsonPrimitive("productRelease").getAsString();
                        GlobalContext globalContext = resource.getGlobalContext();
                        globalContext.setDetectedRelease(productRelease);
                        result.notes = "Revision: " + productRelease;
                        List<String> migrations = new ArrayList<>();
                        object.getAsJsonArray("platformHistory").forEach(e -> {
                            String release = e.getAsJsonObject().getAsJsonPrimitive("release").getAsString();
                            if (!productRelease.equals(release)) {
                                migrations.add(release);
                            }
                        });
                        if (!migrations.isEmpty()) {
                            result.notes += "\nMigrations: " + String.join(", ", migrations);
                        }
                    }
                } else {
                    System.err.println("SupportArchiveParser: 'platform.properties' does not exists");
                }
            } catch (IOException e) {
                System.err.println("SupportArchiveParser: " + e.getMessage());
            }
        }

        @Override
        public DiagnosticResult getResult() {
            return result;
        }
    }
}
