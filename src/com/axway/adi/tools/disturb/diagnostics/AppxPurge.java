package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.AppEntity;
import com.axway.adi.tools.disturb.parsers.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;

import static com.axway.adi.tools.disturb.db.DbConstants.Level.Warning;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.Appx;

public class AppxPurge extends DiagnosticSpecification {

    public AppxPurge() {
        id = "BUILTIN-AP-0003";
        name = "Appx purge";
        setLevel(Warning);
        setResourceType(Appx);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class StatisticsContext extends DiagnosticParseContext<AppIdentifiable> {
        private final Set<String> entityTTL = new HashSet<>();

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void accept(AppIdentifiable item) {
            if (item instanceof AppEntity) {
                AppEntity entity = (AppEntity) item;
                entityTTL.add(entity.ttl.isEmpty() ? "Unlimited" : entity.ttl);
            }
        }

        @Override
        public DiagnosticResult getResult() {
            DiagnosticResult result = buildResult();
            result.notes = "Entity purge settings: " + String.join(", ", entityTTL);
            return result;
        }
    }
}