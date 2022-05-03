package com.axway.adi.tools.disturb.parsers.contexts;

import java.util.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.structures.LogMessage;

public abstract class LogContext extends DiagnosticParseContext<LogMessage> {
    private final Set<String> files = new HashSet<>();

    protected LogContext(DiagnosticSpecification specification, SupportCaseResource resource) {
        super(specification, resource);
    }

    @Override
    public void analyse(String resFile, LogMessage msg) {
        files.add(resFile);
    }

    protected DiagnosticResult update(DiagnosticResult result) {
        files.forEach(f -> result.addItem(f, ""));
        return result;
    }
}
