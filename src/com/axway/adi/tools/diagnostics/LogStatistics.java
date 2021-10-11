package com.axway.adi.tools.diagnostics;

import java.nio.file.Path;
import com.axway.adi.tools.parsers.DiagnosticParseContext;
import com.axway.adi.tools.parsers.LogMessage;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.parsers.LogParser.NODE_LOG;
import static com.axway.adi.tools.util.db.DbConstants.Level.Info;
import static com.axway.adi.tools.util.db.DbConstants.ResourceType.Log;

public class LogStatistics extends DiagnosticSpecification {
    public LogStatistics() {
        id = "BUILTIN-LG-0001";
        name = "Log statistics";
        setLevel(Info);
        setResourceType(Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends DiagnosticParseContext<LogMessage> {
        private int totalCount = 0;
        private int errorCount = 0;
        private int warnCount = 0;

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void accept(LogMessage msg) {
            totalCount++;
            if ("ERROR".equalsIgnoreCase(msg.level)) {
                errorCount++;
            } else if ("WARN".equalsIgnoreCase(msg.level)) {
                warnCount++;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Errors: ");
            sb.append(errorCount);
            sb.append(" , Warnings: ");
            sb.append(warnCount);
            sb.append(" , Total: ");
            sb.append(totalCount);
            result.notes = sb.toString();
            return result;
        }
    }
}
