package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.contexts.LogContext;
import com.axway.adi.tools.disturb.parsers.structures.LogMessage;

import static com.axway.adi.tools.disturb.db.DbConstants.Level.Warning;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.Log;
import static com.axway.adi.tools.disturb.parsers.LogParser.GC_LOG;

public class LogGarbageCollectorAlert extends DiagnosticSpecification {
    private static final int MAX_OCCURRENCES = 4;
    private static final double PAUSE_RATIO = 0.5;
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); //2021-10-06T09:48:36.220+0200

    public LogGarbageCollectorAlert() {
        id = "BUILTIN-LG-0005";
        name = "GC triggers many full pauses";
        description = String.format("Diagnostic triggers if pauses last more than %d%%, more than %d times", (int)(PAUSE_RATIO * 100), MAX_OCCURRENCES);
        remediation = "Increase available memory";
        setLevel(Warning);
        setResourceType(Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class GCPause {
        final LogMessage pauseMsg;
        final Date date;
        double duration;

        GCPause(LogMessage pauseMsg) {
            this.pauseMsg = pauseMsg;
            try {
                date = FORMATTER.parse(pauseMsg.date);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        boolean isSummary(LogMessage msg) {
            return msg.message.startsWith(pauseMsg.message);
        }
        void setDuration(String str) {
            // parse full pause summary:[2021-10-06T09:48:54.732+0200][67485ms] GC(35) Pause Full (Ergonomics) 25272M->25197M(42596M) 5844.486ms
            int pos = str.lastIndexOf(' ');
            if (pos != -1 && str.endsWith("ms")) {
                duration = Double.parseDouble(str.substring(pos + 1, str.length() - 2));
            }
        }
    }

    private static class LogStatisticsContext extends LogContext {
        int count = 0;
        String currentLevel = "";
        GCPause previousPause = null;
        GCPause currentPause = null;

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return GC_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            super.analyse(resFile, msg);
            String level = msg.level;
            if (level == null || level.isEmpty()) {
                return;
            }
            if (!currentLevel.equals(level)) {
                if (currentPause != null) {
                    previousPause = currentPause;
                }
                // new full pause: [2021-10-06T09:48:48.888+0200][61641ms] GC(35) Pause Full (Ergonomics)
                if (msg.message.startsWith("Pause Full")) {
                    currentPause = new GCPause(msg);
                }
                currentLevel = level;
            } else if (currentPause != null && currentPause.isSummary(msg)) {
                // parse full pause summary:[2021-10-06T09:48:54.732+0200][67485ms] GC(35) Pause Full (Ergonomics) 25272M->25197M(42596M) 5844.486ms
                currentPause.setDuration(msg.message);
                // check diagnostic
                if (previousPause != null) {
                    long deltaTime = currentPause.date.getTime() - previousPause.date.getTime();
                    if ((currentPause.duration / deltaTime) > PAUSE_RATIO) {
                        count++;
                    }
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (count <= MAX_OCCURRENCES) {
                return null; // acceptable
            }
            DiagnosticResult result = buildResult();
            result.notes = String.format("GC paused more than %d%%, %d times", (int) (PAUSE_RATIO * 100), count);
            return update(result);
        }
    }
}
