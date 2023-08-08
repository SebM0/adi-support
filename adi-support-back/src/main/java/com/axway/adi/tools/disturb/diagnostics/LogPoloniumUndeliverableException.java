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

import static com.axway.adi.tools.disturb.parsers.LogParser.*;

public class LogPoloniumUndeliverableException extends DiagnosticSpecification {

    public LogPoloniumUndeliverableException() {
        id = "BUILTIN-LG-0009";
        name = "Polonium exception";
        description = "Polonium raised exception after query execution";
        setLevel(DbConstants.Level.Error);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends LogContext {
        Set<String> stacks = new HashSet<>();

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath) || INTEGRATION_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            if (msg.domain.startsWith("Rx") && msg.message.contains("Uncaught exception") && msg.dump != null && !msg.dump.isEmpty() && msg.dump.stream().anyMatch(s -> s.contains("polonium"))) {
                stacks.add(msg.formatDump());
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (!stacks.isEmpty()) {
                DiagnosticResult result = buildResult();
                result.notes = "" + stacks.size() + " polonium uncaught exception detected";
                stacks.forEach(f -> result.addItem(f, ""));
                return result;
            }
            return null;
        }
    }
}
