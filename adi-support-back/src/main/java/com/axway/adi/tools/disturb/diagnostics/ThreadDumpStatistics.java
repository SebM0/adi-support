package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.ThreadDump;

public class ThreadDumpStatistics extends DiagnosticSpecification {

    public ThreadDumpStatistics() {
        id = "BUILTIN-TD-0001";
        name = "Thread Dumps statistics";
        setLevel(DbConstants.Level.Info);
        setResourceType(DbConstants.ResourceType.ThreadDump);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new ThreadDumpStatisticsContext(this, res);
    }

    private static class ThreadDumpStatisticsContext extends DiagnosticParseContext<ThreadDump> {
        private int totalCount = 0;
        private Map<String,Integer> countByComponent = new HashMap<>();

        protected ThreadDumpStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, ThreadDump threadDump) {
            totalCount++;
            String threadComponent = threadDump.getThreadComponent();
            if (threadComponent != null) {
                countByComponent.compute(threadComponent, (k, count) -> count == null ? 1 : (count + 1));
            }
        }

        @Override
        public DiagnosticResult getResult() {
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Number of threads: ");
            sb.append(totalCount);
            result.notes = sb.toString();
            countByComponent.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> result.addItem(e.getKey(), e.getValue().toString()));
            return result;
        }
    }
}
