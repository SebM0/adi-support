package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.AppIndicator;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;

import static com.axway.adi.tools.disturb.db.DbConstants.Level.Error;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.Appx;
import static java.util.stream.Collectors.*;

public class AppxObsoleteTypes extends DiagnosticSpecification {
    private static final Set<String> OBSOLETE_TYPES = Set.of("Baseline", "ThresholdLevel", "BaselineThresholdMultiplier");

    public AppxObsoleteTypes() {
        id = "BUILTIN-AP-0002";
        name = "Appx obsolete types";
        setLevel(Error);
        setResourceType(Appx);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class StatisticsContext extends DiagnosticParseContext<AppIdentifiable> {
        private final Map<String, Integer> statsByType = new HashMap<>();

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void accept(AppIdentifiable item) {
            if (item instanceof AppIndicator) {
                AppIndicator attr = (AppIndicator) item;
                if (OBSOLETE_TYPES.contains(attr.type)) {
                    statsByType.compute(attr.type, (type, count) -> count == null ? 1 : (count + 1));
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (statsByType.isEmpty()) {
                return null;
            }
            DiagnosticResult result = buildResult();
            result.notes = statsByType.entrySet().stream() //
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(joining(" "));
            return result;
        }
    }
}
