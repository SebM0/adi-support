package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Path;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.LogMessage;

import static com.axway.adi.tools.disturb.parsers.LogParser.NODE_LOG;

public class LogLVStorageNotActivated extends DiagnosticSpecification {

    public LogLVStorageNotActivated() {
        id = "BUILTIN-LG-0004";
        name = "LV storage not activated";
        description = "Low Volume storage not activated. It cause performance issues at startup and model changes";
        remediation = "Activate it with property 'com.systar.titanium.lowVolumeColumn=forceOn'";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends DiagnosticParseContext<LogMessage> {
        String detected = null;

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void accept(LogMessage msg) {
            if ("Low Volume persistence system is not activated".equalsIgnoreCase(msg.message)) {
                detected = msg.date;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (detected != null) {
                DiagnosticResult result = buildResult();
                result.notes = "Last detected at " + detected;
                return result;
            }
            return null;
        }
    }
}
