package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.activity.RedoStatus;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.contexts.LogContext;
import com.axway.adi.tools.disturb.parsers.structures.LogMessage;

import static com.axway.adi.tools.disturb.parsers.LogParser.ACTIVITY_LOG;

public class LogActivityRedoOutOfSync extends DiagnosticSpecification {
    private static final int SYNC_THRESHOLD = 80;
    private static final int WARN_THRESHOLD = 10;

    public LogActivityRedoOutOfSync() {
        id = "BUILTIN-LG-0007";
        name = "HA out of sync";
        description = "HA consume redo log too slowly";
        remediation = "Buy a better network";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends LogContext {
        private static final long NO_DATA = Long.MAX_VALUE;
        LinkedHashMap<String,String> syncWarnings = new LinkedHashMap<>();

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return ACTIVITY_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            super.analyse(resFile, msg);
            RedoStatus mainRedoStatus = RedoStatus.readFromJson("main", msg.args);
            if (mainRedoStatus != null) {
                // search for replicas
                List<RedoStatus> consumerRedoStatuses = RedoStatus.searchConsumers(msg.args);
                if (!consumerRedoStatuses.isEmpty()) {
                    long worseDbOffset = NO_DATA;
                    long worseWfOffset = NO_DATA;
                    boolean badCheckpoint = false;
                    for (RedoStatus consumerStatus : consumerRedoStatuses) {
                        if (mainRedoStatus.getCheckpoint() != consumerStatus.getCheckpoint()) {
                            badCheckpoint = true;
                            break;
                        }
                        worseDbOffset = Math.min(worseDbOffset, consumerStatus.getDbOffset());
                        if (consumerStatus.getWfOffset() != -1) {
                            worseWfOffset = Math.min(worseWfOffset, consumerStatus.getWfOffset());
                        }
                    }
                    long dbSync = mainRedoStatus.getDbOffset() > 0 && worseDbOffset != NO_DATA ? 100 * worseDbOffset / mainRedoStatus.getDbOffset() : NO_DATA;
                    long wfSync = mainRedoStatus.getWfOffset() > 0 && worseWfOffset != NO_DATA ? 100 * worseWfOffset / mainRedoStatus.getWfOffset() : NO_DATA;
                    if (badCheckpoint || dbSync < SYNC_THRESHOLD || wfSync < SYNC_THRESHOLD) {
                        log(msg.date, mainRedoStatus, consumerRedoStatuses);
                    }
                }
            }
        }

        private void log(String time, RedoStatus mainRedoStatus, List<RedoStatus> consumerRedoStatuses) {
            StringBuilder sb = new StringBuilder();
            sb.append(mainRedoStatus);
            consumerRedoStatuses.forEach(c -> sb.append("\n").append(c));
            syncWarnings.put(time, sb.toString());
        }

        @Override
        public DiagnosticResult getResult() {
            if (syncWarnings.size() >= WARN_THRESHOLD) {
                DiagnosticResult result = buildResult();
                result.notes = syncWarnings.size() + " redo log synchronization issues detected";
                syncWarnings.forEach(result::addItem);
                return result;
            }
            return null;
        }
    }
}
