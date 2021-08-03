package com.axway.adi.tools.diagnostics;

import com.axway.adi.tools.parsers.DiagnosticParseContext;
import com.axway.adi.tools.parsers.ThreadDumpContext;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.util.db.DbConstants.Level.Info;
import static com.axway.adi.tools.util.db.DbConstants.ResourceType.ThreadDump;

public class ThreadDumpStatistics extends DiagnosticSpecification {

    public ThreadDumpStatistics() {
        id = "BUILTIN-TD-0001";
        name = "Thread Dumps statistics";
        setLevel(Info);
        setResourceType(ThreadDump);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new ThreadDumpStatisticsContext(this, res);
    }

    private static class ThreadDumpStatisticsContext extends ThreadDumpContext {
        private int totalCount = 0;

        protected ThreadDumpStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void accept(com.axway.adi.tools.parsers.ThreadDump threadDump) {
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
