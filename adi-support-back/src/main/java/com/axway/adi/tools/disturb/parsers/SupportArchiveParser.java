package com.axway.adi.tools.disturb.parsers;

import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.SupportArchive;
import static java.util.stream.Collectors.*;

public class SupportArchiveParser extends Parser {

    public SupportArchiveParser(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    protected void parseDirectory(Path filePath, Consumer<DiagnosticResult> resultConsumer) {
        // Create diagnostic contexts
        List<DiagnosticParseContext<Path>> diagnosticContexts = CAT.getDiagnosticsByType(SupportArchive).stream() //
                .map(this::createDiagnosticContext) //
                .collect(toList());

        diagnosticContexts.forEach(context -> {
            context.analyse(filePath.getFileName().toString(), filePath);
            DiagnosticResult result = context.getResult();
            if (result != null) {
                resultConsumer.accept(result);
            }
        });
    }

    @Override
    protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) {
        // Skip
    }

    @SuppressWarnings("unchecked")
    private DiagnosticParseContext<Path> createDiagnosticContext(DiagnosticSpecification diag) {
        return (DiagnosticParseContext<Path>) diag.createContext(resource);
    }

    @Override
    public int getSize() {
        return 0;
    }
}
