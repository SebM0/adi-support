package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.ThreadDump;

public class ThreadDumpBlocked extends DiagnosticSpecification {
    private static final int MAX_BLOCKED = 2;

    public ThreadDumpBlocked() {
        id = "BUILTIN-TD-0004";
        name = "Blocked";
        description = "Diagnostic triggers when more than " + MAX_BLOCKED + " threads are blocked";
        remediation = "If blocked on Model reading, consider activating LV";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.ThreadDump);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new ThreadDumpStatisticsContext(this, res);
    }

    private static class ThreadDumpStatisticsContext extends DiagnosticParseContext<ThreadDump> {
        private final Map<String, Integer> blockedByLocker = new HashMap<>();
        private final Map<String, ThreadDump> blockingByLocker = new HashMap<>();

        protected ThreadDumpStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, ThreadDump threadDump) {
            if (threadDump.locks.isEmpty()) {
                return;
            }
            if (threadDump.status.startsWith("BLOCKED") && threadDump.locks.get(0).toLowerCase().contains("waiting")) {
                String locker = getLocker(threadDump.locks.get(0));
                blockedByLocker.compute(locker, (type, count) -> count == null ? 1 : (count + 1));
            } else {
                threadDump.locks.stream().filter(l -> l.toLowerCase().contains("locked")).map(this::getLocker).forEach(l -> blockingByLocker.put(l, threadDump));
            }
        }

        private String getLocker(String lockMessage) {
            String locker = "<unknown>";
            int lockerStart = lockMessage.indexOf('<');
            int lockerEnd = lockMessage.indexOf('>');
            if (lockerStart != -1 && lockerEnd != -1 && lockerEnd > lockerStart) {
                locker = lockMessage.substring(lockerStart, lockerEnd + 1);
            }
            return locker;
        }

        @Override
        public DiagnosticResult getResult() {
            int totalCount = blockedByLocker.values().stream().mapToInt(count -> count).sum();
            if (totalCount > MAX_BLOCKED) {
                DiagnosticResult result = buildResult();
                result.notes = "Blocked threads detected " + totalCount + " times";
                blockedByLocker.forEach((locker, count) -> {
                    ThreadDump lockerThread = blockingByLocker.get(locker);
                    result.notes += "\n - " + count + " times by " + (lockerThread != null ? lockerThread.name : locker);
                    if (lockerThread != null) {
                        result.addItem(locker, lockerThread.toString());
                    }
                });
                return result;
            }
            return null;
        }
    }
}
