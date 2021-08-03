package com.axway.adi.tools.parsers;

import java.util.function.*;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

public abstract class DiagnosticParseContext<T> implements Consumer<T> {
    private final DiagnosticSpecification specification;
    private final SupportCaseResource resource;

    protected DiagnosticParseContext(DiagnosticSpecification specification, SupportCaseResource resource) {
        this.specification = specification;
        this.resource = resource;
    }

    public abstract DiagnosticResult getResult();

    protected DiagnosticResult buildResult() {
        return specification.createResult(resource);
    }
}
