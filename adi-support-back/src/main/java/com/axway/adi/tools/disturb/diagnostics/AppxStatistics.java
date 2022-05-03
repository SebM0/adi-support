package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.AppEntity;
import com.axway.adi.tools.disturb.parsers.structures.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.structures.AppIndicator;

public class AppxStatistics extends DiagnosticSpecification {
    public AppxStatistics() {
        id = "BUILTIN-AP-0001";
        name = "Appx statistics";
        setLevel(DbConstants.Level.Info);
        setResourceType(DbConstants.ResourceType.Appx);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class StatisticsContext extends DiagnosticParseContext<AppIdentifiable> {
        private final Map<String,Integer> indicatorByType = new HashMap<>();
        private int entityCount = 0;

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, AppIdentifiable item) {
            if (item instanceof AppEntity) {
                entityCount++;
            } else if (item instanceof AppIndicator) {
                AppIndicator attr = (AppIndicator)item;
                if (!"Instance".equals(attr.type)) {
                    indicatorByType.compute(attr.type, (type, count) -> count == null ? 1 : (count + 1));
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            DiagnosticResult result = buildResult();
            result.notes = "Entities: " + entityCount + ", Attributes: " + indicatorByType.values().stream().mapToInt(i -> i).sum();
            indicatorByType.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> result.addItem(e.getKey(), e.getValue().toString()));
            return result;
        }
    }
}
