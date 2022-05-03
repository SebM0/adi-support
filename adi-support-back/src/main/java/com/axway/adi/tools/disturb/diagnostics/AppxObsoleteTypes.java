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
import com.axway.adi.tools.disturb.parsers.structures.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.structures.AppIndicator;

import static java.util.stream.Collectors.*;

public class AppxObsoleteTypes extends DiagnosticSpecification {
    private static final Set<String> OBSOLETE_TYPES = Set.of("Baseline", "ThresholdLevel", "BaselineThresholdMultiplier");
    private static final Set<String> OBSOLETE_CONFS = Set.of("BaselineIndicatorConfiguration", "ClassifierBaselineIndicatorConfiguration", "ClassifierLevelIndicatorConfiguration");

    public AppxObsoleteTypes() {
        id = "BUILTIN-AP-0002";
        name = "Appx obsolete types";
        setLevel(DbConstants.Level.Error);
        setResourceType(DbConstants.ResourceType.Appx);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class StatisticsContext extends DiagnosticParseContext<AppIdentifiable> {
        private final Map<String, List<AppIndicator>> attributesByType = new HashMap<>();

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, AppIdentifiable item) {
            if (item instanceof AppIndicator) {
                AppIndicator attr = (AppIndicator) item;
                if (OBSOLETE_TYPES.contains(attr.type)) {
                    attributesByType.computeIfAbsent(attr.type, type -> new ArrayList<>()).add(attr);
                }
                if (!attr.configuration.isEmpty()) {
                    Path configurationPath = Path.of(this.resource.getAnalysisPath(), attr.configuration);
                    try {
                        String configuration = Files.readString(configurationPath);
                        for (String obsolete : OBSOLETE_CONFS) {
                            if (configuration.contains((obsolete))) {
                                attributesByType.computeIfAbsent(obsolete, type -> new ArrayList<>()).add(attr);
                                return;
                            }
                        }
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (attributesByType.isEmpty()) {
                return null;
            }
            DiagnosticResult result = buildResult();
            result.notes = attributesByType.entrySet().stream() //
                    .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(joining(" "));
            return result;
        }
    }
}
