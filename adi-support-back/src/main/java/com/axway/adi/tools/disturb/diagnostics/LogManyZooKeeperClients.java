package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.LogMessage;

import static com.axway.adi.tools.disturb.parsers.LogParser.NODE_LOG;

public class LogManyZooKeeperClients extends DiagnosticSpecification {
    private static final int MAX_CONSUMERS = 4;
    private static final String ADDRESS_TAG = "address = ";

    public LogManyZooKeeperClients() {
        id = "BUILTIN-LG-0003";
        name = "Log has many ZooKeeper";
        description = "Diagnostic triggers if more than " + MAX_CONSUMERS + " clients";
        remediation = "Make sure it is the expected behavior";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.Log);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new LogStatisticsContext(this, res);
    }

    private static class LogStatisticsContext extends DiagnosticParseContext<LogMessage> {
        Set<String> consumers = new HashSet<>();

        protected LogStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public boolean filter(Path filePath) {
            return NODE_LOG.test(filePath);
        }

        @Override
        public void accept(LogMessage msg) {
            if (msg.domain.contains("zookeeper") && "Unexpected exception".equalsIgnoreCase(msg.message) && msg.dump != null && !msg.dump.isEmpty()) {
                String details = msg.dump.get(0);
                if (details.contains("EndOfStreamException")) {
                    int start = details.indexOf(ADDRESS_TAG);
                    if (start > 0) {
                        start += ADDRESS_TAG.length();
                        int end = details.indexOf(",", start);
                        if (end > 0) {
                            String node = details.substring(start, end);
                            consumers.add(node);
                        }
                    }
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (consumers.size() <= MAX_CONSUMERS) {
                return null; // acceptable
            }
            DiagnosticResult result = buildResult();
            StringBuilder sb = new StringBuilder();
            sb.append(consumers.size());
            sb.append(" client detected: ");
            sb.append(String.join(", ", consumers));
            result.notes = sb.toString();
            return result;
        }
    }
}
