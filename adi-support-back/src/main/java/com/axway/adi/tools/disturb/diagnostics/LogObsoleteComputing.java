package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.contexts.LogContext;
import com.axway.adi.tools.disturb.parsers.structures.LogMessage;

import static com.axway.adi.tools.disturb.parsers.LogParser.NODE_LOG;

public class LogObsoleteComputing extends DiagnosticSpecification {

    public LogObsoleteComputing() {
        id = "BUILTIN-LG-0006";
        name = "Obsolete computing";
        description = "Computing has unsupported configuration";
        remediation = "Update application to replace old configurations by new ones";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends LogContext {
        Map<String,Set<String>> consumers = new HashMap<>();

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            if ("Unsupported computing configuration".equalsIgnoreCase(msg.message)) {
                super.analyse(resFile, msg);
                String name = msg.args.getAsJsonPrimitive("indicatorName").getAsString();
                String vt = msg.args.getAsJsonPrimitive("interval").getAsString();
                consumers.computeIfAbsent(name, n -> new HashSet<>()).add(vt);
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (consumers.isEmpty()) {
                return null; // acceptable
            }
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append(consumers.size());
            sb.append(" obsolete configurations detected: ");
            result.notes = sb.toString();
            consumers.forEach((name, vts) -> result.addItem(name, String.join(", ", vts)));
            return result;
        }
    }
}
