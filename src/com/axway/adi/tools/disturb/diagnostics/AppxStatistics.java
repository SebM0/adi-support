package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.AppEntity;
import com.axway.adi.tools.disturb.parsers.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.AppIndicator;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;

import static com.axway.adi.tools.disturb.db.DbConstants.Level.Info;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.Appx;

public class AppxStatistics extends DiagnosticSpecification {
    public AppxStatistics() {
        id = "BUILTIN-AP-0001";
        name = "Appx statistics";
        setLevel(Info);
        setResourceType(Appx);
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
        public void accept(AppIdentifiable item) {
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
            StringBuilder sb = new StringBuilder();
            sb.append("Entities: ").append(entityCount);
            sb.append("\nAttributes: ").append(indicatorByType.values().stream().mapToInt(i -> i).sum());
            indicatorByType.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> sb.append("\n  ").append(e.getKey()).append(": ").append(e.getValue()));
            result.notes = sb.toString();
            return result;
        }
    }
}
