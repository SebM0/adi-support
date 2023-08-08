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

public class LogStatistics extends DiagnosticSpecification {
    public LogStatistics() {
        id = "BUILTIN-LG-0001";
        name = "Log statistics";
        setLevel(DbConstants.Level.Info);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogInterval {
        private final String startDate;
        private String endDate;

        LogInterval(String date) {
            startDate = date;
        }
        void increase(String date) {
            endDate = date;
        }

        @Override
        public String toString() {
            return startDate + " - " + endDate;
        }
    }

    private static class LogStatisticsContext extends LogContext {
        private static final Set<String> FATAL_ERRORS = Set.of("Platform did not start correctly, stopping it now", "Unrecoverable error found");
        private int totalCount = 0;
        private int fatalCount = 0;
        private int errorCount = 0;
        private int warnCount = 0;
        private int sessionCount = 0;
        private Map<String, LogInterval> fileDates = new HashMap<>();

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            fileDates.computeIfAbsent(resFile, file -> new LogInterval(msg.date)).increase(msg.date);
            totalCount++;
            if ("ERROR".equalsIgnoreCase(msg.level)) {
                if (FATAL_ERRORS.stream().anyMatch(fatal -> msg.message.contains(fatal))) {
                    fatalCount++;
                } else {
                    errorCount++;
                }
            } else if ("WARN".equalsIgnoreCase(msg.level)) {
                warnCount++;
            }
            if ("platform".equals(msg.component) && msg.message.contains("JVM INFORMATION")) {
                sessionCount++;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            if (fatalCount > 0) {
                sb.append("Fatals: ");
                sb.append(fatalCount);
                sb.append(" , ");
            }
            sb.append("Errors: ");
            sb.append(errorCount);
            sb.append(" , Warnings: ");
            sb.append(warnCount);
            sb.append(" , Total: ");
            sb.append(totalCount);
            sb.append("\n New sessions: ");
            sb.append(sessionCount);
            result.notes = sb.toString();
            fileDates.forEach((f, interval) -> result.addItem(f, interval.toString()));
            return result;
        }
    }
}
