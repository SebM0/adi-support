package com.axway.adi.tools.disturb.parsers;

import java.nio.file.Path;
import java.util.function.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

public abstract class DiagnosticParseContext<T> implements Consumer<T> {
    private final DiagnosticSpecification specification;
    private final SupportCaseResource resource;

    protected DiagnosticParseContext(DiagnosticSpecification specification, SupportCaseResource resource) {
        this.specification = specification;
        this.resource = resource;
    }

    public abstract DiagnosticResult getResult();

    public boolean filter(Path filePath) {
        return true; // accept all by default
    }

    protected DiagnosticResult buildResult() {
        return specification.createResult(resource);
    }
}
