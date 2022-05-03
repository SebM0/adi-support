package com.axway.adi.tools.disturb.diagnostics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.util.TimeUtils;

import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.SupportArchive;

public class SupportArchiveCheckConfiguration extends DiagnosticSpecification {

    public SupportArchiveCheckConfiguration() {
        id = "BUILTIN-SA-0004";
        name = "Configuration";
        setLevel(DbConstants.Level.Warning);
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
                Path propertiesPath = path.resolve("conf/platform.properties");
                if (Files.isRegularFile(propertiesPath)) {
                    Properties platformProperties = new Properties();
                    try (FileReader fileReader = new FileReader(propertiesPath.toFile())) {
                        platformProperties.load(fileReader);
                    }
                    // Check late data
                    String lateTTL = platformProperties.getProperty("com.systar.krypton.scheduler.lateDataHandler.maximumTimeToLive");
                    if (lateTTL != null && !lateTTL.isBlank()) {
                        long duration = 15 * 60 * 1000; //15 minutes by default
                        // parse
                        try {
                            // duration in millis
                            duration = Long.parseLong(lateTTL);
                        } catch (NumberFormatException e) {
                            // Formatted duration: "15 minutes"
                            String[] split = lateTTL.split(" ");
                            if (split.length == 2) {
                                duration = Long.parseLong(split[0]);
                                duration = TimeUtils.computeDuration(duration, split[1]);
                            }
                        }
                        if (duration < 5 * 60 * 1000) { // Warning threshold at 5 minutes
                            result = buildResult();
                            result.notes = "LateData maximumTimeToLive is low: " + lateTTL;
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
