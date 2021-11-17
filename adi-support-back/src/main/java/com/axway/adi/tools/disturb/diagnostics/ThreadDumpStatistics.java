package com.axway.adi.tools.disturb.diagnostics;

import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
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

        protected ThreadDumpStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, ThreadDump threadDump) {
            totalCount++;
        }

        @Override
        public DiagnosticResult getResult() {
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append("Number of threads: ");
            sb.append(totalCount);
            result.notes = sb.toString();
            return result;
        }
    }
}
