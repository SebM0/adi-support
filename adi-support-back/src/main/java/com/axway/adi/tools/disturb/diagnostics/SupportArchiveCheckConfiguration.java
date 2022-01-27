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

    private static class IndicatorReport {
        String name;
        long liveCount = 0;
        long recomputingCount = 0;
        double getRatio() {
            return liveCount == 0 && recomputingCount == 0 ? 0.0 : (recomputingCount * 100.0) / ((double) (recomputingCount + liveCount));
        }
        public String toString() {
            return name + " : " + String.format("%.4f", getRatio()) + " (" + recomputingCount + " vs " + liveCount + ")";
        }
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
                    platformProperties.load(new FileReader(propertiesPath.toFile()));
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
                                switch (split[1]) {
                                    case "days":
                                        duration *= 24;
                                        // no break;
                                    case "hours":
                                        duration *= 60;
                                        // no break;
                                    case "minutes":
                                        duration *= 60;
                                        // no break;
                                    case "seconds":
                                        duration *= 1000;
                                        break;
                                }
                            }
                        }
                        if (duration < 5 * 60 * 1000) { // Warning threashold at 5 minutes
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
