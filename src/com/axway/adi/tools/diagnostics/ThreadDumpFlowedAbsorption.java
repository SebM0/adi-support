package com.axway.adi.tools.diagnostics;

import java.util.*;
import com.axway.adi.tools.parsers.DiagnosticParseContext;
import com.axway.adi.tools.parsers.ThreadDump;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.util.db.DbConstants.Level.Warning;
import static com.axway.adi.tools.util.db.DbConstants.ResourceType.ThreadDump;

public class ThreadDumpFlowedAbsorption extends DiagnosticSpecification {
    private static final int MAX_WAITING = 4;
    private static final List<String> WAITING_TRACES = List.of("absorption.clock.VectorClockImpl.awaitVectorClockProgress",
                                                               "tau.util.concurrent.ChannelSemaphore.acquire");

    public ThreadDumpFlowedAbsorption() {
        id = "BUILTIN-TD-0003";
        name = "Busy absorption";
        description = "Diagnostic triggers when more than " + MAX_WAITING + " threads are waiting for absorption";
        remediation = "Investigate how to alleviate absorption or increase channel capacity";
        setLevel(Warning);
        setResourceType(ThreadDump);
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
        public void accept(ThreadDump threadDump) {
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
