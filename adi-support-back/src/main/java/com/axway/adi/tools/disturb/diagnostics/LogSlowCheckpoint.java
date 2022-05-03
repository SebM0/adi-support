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
import com.google.gson.JsonPrimitive;

import static com.axway.adi.tools.disturb.parsers.LogParser.NODE_LOG;

public class LogSlowCheckpoint extends DiagnosticSpecification {
    private static final long LONG_CHECKPOINT = 2 * 60 * 1000; // 2 minutes

    public LogSlowCheckpoint() {
        id = "BUILTIN-LG-0008";
        name = "Checkpoint performance";
        description = "Checkpoint are taking longer";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends LogContext {
        Date checkpointStart = null;
        int checkpointCount = 0;
        int longCheckpointCount = 0;

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            if ("Creation of checkpoint is in progress".equalsIgnoreCase(msg.message) || "Transactions will now be consumed from redo log".equalsIgnoreCase(msg.message)) {
                // Start checkpoint on main or backup
                checkpointStart = msg.parseDate();
            } else if ("Checkpoint was created".equalsIgnoreCase(msg.message)) {
                super.analyse(resFile, msg);
                checkpointCount++;
                JsonPrimitive durationField = msg.args.getAsJsonPrimitive("durationInMs");
                long duration = 0 ;
                if (durationField == null) {
                    Date checkpointEnd = msg.parseDate();
                    if (checkpointEnd != null && checkpointStart != null) {
                        duration = checkpointEnd.getTime() - checkpointStart.getTime();
                    }
                } else {
                    duration = durationField.getAsLong();
                }
                if (duration >= LONG_CHECKPOINT) {
                    longCheckpointCount++;
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (longCheckpointCount == 0) {
                return null; // acceptable
            }
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append(longCheckpointCount);
            sb.append(" / ");
            sb.append(checkpointCount);
            sb.append(" slow checkpoints detected");
            result.notes = sb.toString();
            return update(result);
        }
    }
}
