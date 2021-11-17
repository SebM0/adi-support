package com.axway.adi.tools.disturb.diagnostics;

import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.ThreadDump;

public class ThreadDumpLowPerformancePlanOperators extends DiagnosticSpecification {
    private static final String SLOW_OPERATOR = "InstantCompositeInstanceIdJoinPhysicalOperator";

    public ThreadDumpLowPerformancePlanOperators() {
        id = "BUILTIN-TD-0002";
        name = "Low performance query plan operator";
        description = "Low performance query plan operator detected: [" + SLOW_OPERATOR + "]";
        remediation = "Migrate to release xxx or above";
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
            if (threadDump.getStackTrace().contains(SLOW_OPERATOR)) {
                totalCount++;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (totalCount > 0) {
                DiagnosticResult result = buildResult();
                result.notes = "Slow operator detected " + totalCount + " times";
                return result;
            }
            return null;
        }
    }
}
