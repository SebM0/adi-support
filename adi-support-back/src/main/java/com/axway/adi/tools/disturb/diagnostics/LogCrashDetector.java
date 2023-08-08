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
import com.axway.adi.tools.util.CappedList;

import static com.axway.adi.tools.disturb.parsers.LogParser.NODE_LOG;
import static java.util.stream.Collectors.*;

public class LogCrashDetector extends DiagnosticSpecification {
    private static final int MAX_MESSAGES_AFTER_STOP = 12;

    public LogCrashDetector() {
        id = "BUILTIN-LG-0010";
        name = "Crash detected";
        description = "Node was not stopped gracefully";
        remediation = "Make sure it is the expected behavior";
        setLevel(DbConstants.Level.Error);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends LogContext {
        List<Crash> crashDetected = new ArrayList<>();
        List<LogMessage> lastMessages = new CappedList<>(MAX_MESSAGES_AFTER_STOP);
        Integer messagesSinceStop = 0;

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void analyse(String resFile, LogMessage msg) {
            if ("platform".equals(msg.component) && "Platform stopped".equalsIgnoreCase(msg.message)) {
                messagesSinceStop = 0;
                lastMessages.clear();
            }
            else if ("platform".equals(msg.component) && msg.message.contains("JVM INFORMATION") && messagesSinceStop != null) {
                if (messagesSinceStop > MAX_MESSAGES_AFTER_STOP) {
                    crashDetected.add(new Crash(lastMessages, resFile));
                }
                messagesSinceStop = 0;
            }
            else {
                lastMessages.add(msg);
                if (messagesSinceStop != null) {
                    messagesSinceStop++;
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (crashDetected.isEmpty()) {
                return null; // acceptable
            }
            List<String> dates = crashDetected.stream().map(crash -> crash.lastMessages.get(crash.lastMessages.size()-1).date).collect(toList());
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append(crashDetected.size());
            sb.append(" crashes detected: ");
            sb.append(String.join(", ", dates));
            result.notes = sb.toString();
            crashDetected.forEach(crash -> result.addItem(crash.lastMessages.stream().map(Object::toString).collect(joining("\n")), crash.resFile));
            return result;
        }
    }

    private static class Crash {
        final List<LogMessage> lastMessages;
        final String resFile;

        private Crash(List<LogMessage> lastMessages, String resFile) {
            this.lastMessages = List.copyOf(lastMessages);
            this.resFile = resFile;
        }
    }

}
