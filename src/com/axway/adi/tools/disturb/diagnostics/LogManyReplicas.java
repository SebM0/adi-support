package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.LogMessage;

import static com.axway.adi.tools.disturb.db.DbConstants.Level.Warning;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.Log;
import static com.axway.adi.tools.disturb.parsers.LogParser.NODE_LOG;

public class LogManyReplicas extends DiagnosticSpecification {
    private static final int MAX_CONSUMERS = 4;

    public LogManyReplicas() {
        id = "BUILTIN-LG-0002";
        name = "Log has many replicas";
        description = "Diagnostic triggers if more than " + MAX_CONSUMERS + " replicas";
        remediation = "Make sure it is the expected behavior";
        setLevel(Warning);
        setResourceType(Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends DiagnosticParseContext<LogMessage> {
        Set<String> consumers = new HashSet<>();

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void accept(LogMessage msg) {
            if ("Consumer node is now consuming redo log".equalsIgnoreCase(msg.message)) {
                String node = msg.args.getAsJsonPrimitive("node").getAsString();
                consumers.add(node);
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (consumers.size() <= MAX_CONSUMERS) {
                return null; // acceptable
            }
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append(consumers.size());
            sb.append(" replicas detected: ");
            sb.append(String.join(", ", consumers));
            result.notes = sb.toString();
            return result;
        }
    }
}
