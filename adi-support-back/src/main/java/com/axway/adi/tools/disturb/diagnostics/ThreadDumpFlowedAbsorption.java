package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.ThreadDump;

public class ThreadDumpFlowedAbsorption extends DiagnosticSpecification {
    private static final int MAX_WAITING = 4;
    private static final List<String> WAITING_TRACES = List.of("absorption.clock.VectorClockImpl.awaitVectorClockProgress",
                                                               "tau.util.concurrent.ChannelSemaphore.acquire");

    public ThreadDumpFlowedAbsorption() {
        id = "BUILTIN-TD-0003";
        name = "Busy absorption";
        description = "Diagnostic triggers when more than " + MAX_WAITING + " threads are waiting for absorption";
        remediation = "Investigate how to alleviate absorption or increase channel capacity";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.ThreadDump);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new ThreadDumpStatisticsContext(this, res);
    }

    private static class ThreadDumpStatisticsContext extends DiagnosticParseContext<ThreadDump> {
        private int totalCount = 0;

        protected ThreadDumpStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, ThreadDump threadDump) {
            String stackTrace = threadDump.getStackTrace();
            if (WAITING_TRACES.stream().anyMatch(stackTrace::contains)) {
                totalCount++;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (totalCount > MAX_WAITING) {
                DiagnosticResult result = buildResult();
                result.notes = "Waiting threads detected " + totalCount + " times";
                return result;
            }
            return null;
        }
    }
}
