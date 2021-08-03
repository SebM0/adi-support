package com.axway.adi.tools.parsers;

import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

public class ThreadDumpContext extends DiagnosticParseContext<ThreadDump> {
    private int totalCount = 0;

    protected ThreadDumpContext(DiagnosticSpecification specification, SupportCaseResource resource) {
        super(specification, resource);
    }

    @Override
    public DiagnosticResult getResult() {
        return null;  // TODO implement method
    }

    @Override
    public void accept(ThreadDump threadDump) {
        totalCount++;
    }
}
