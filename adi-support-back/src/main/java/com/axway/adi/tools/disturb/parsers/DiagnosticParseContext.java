package com.axway.adi.tools.disturb.parsers;

import java.nio.file.Path;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

public abstract class DiagnosticParseContext<T> {
    private final DiagnosticSpecification specification;
    protected final SupportCaseResource resource;

    protected DiagnosticParseContext(DiagnosticSpecification specification, SupportCaseResource resource) {
        this.specification = specification;
        this.resource = resource;
    }

    public abstract void analyse(String resFile, T t);

    public abstract DiagnosticResult getResult();

    public boolean filter(Path filePath) {
        return true; // accept all by default
    }

    protected DiagnosticResult buildResult() {
        return specification.createResult(resource);
    }
}
