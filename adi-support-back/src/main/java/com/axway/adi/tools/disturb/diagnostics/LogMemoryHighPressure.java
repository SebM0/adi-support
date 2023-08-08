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

import static com.axway.adi.tools.disturb.parsers.LogParser.MEMORY_LOG;

public class LogMemoryHighPressure extends DiagnosticSpecification {
    private static final int PRESSURE_THRESHOLD = 80;
    private static final int WARN_THRESHOLD = 3;

    public LogMemoryHighPressure() {
        id = "BUILTIN-LG-0011";
        name = "Memory high pressure";
        description = "Node uses too much memory";
        remediation = "Buy some RAM";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends LogContext {
        List<LogMessage> pressureWarnings = new ArrayList<>();
        int count = 0;
        LogMessage detectedPressure = null;


        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return MEMORY_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            super.analyse(resFile, msg);
            long consumption = getMemoryUsage(msg);
            if (consumption >= PRESSURE_THRESHOLD) {
                count++;
                if (detectedPressure == null) {
                    detectedPressure = msg;
                }
            } else {
                validate();
            }
        }

        private void validate() {
            if (count > WARN_THRESHOLD) {
                pressureWarnings.add(detectedPressure);
                detectedPressure = null;
            }
            count = 0;
        }

        private static long getMemoryUsage(LogMessage msg) {
            long jvmUsed = msg.args.get("jvmUsed").getAsLong();
            long jvmMax = msg.args.get("jvmMax").getAsLong();
            return jvmUsed * 100L / jvmMax;
        }

        @Override
        public DiagnosticResult getResult() {
            validate();
            if (!pressureWarnings.isEmpty()) {
                DiagnosticResult result = buildResult();
                result.notes = pressureWarnings.size() + " memory high pressure detected";
                pressureWarnings.forEach(w -> result.addItem(String.format("%02d %%", getMemoryUsage(w)), w.toString()));
                return result;
            }
            return null;
        }
    }
}
