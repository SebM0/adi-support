package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.FileDescription;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static java.util.stream.Collectors.*;

public class FileListParser extends CsvParser {
    int count = 0;
    List<DiagnosticParseContext<FileDescription>> diagnosticContexts;

    public FileListParser(SupportCaseResource resource) {
        super(resource, true);
    }

    @Override
    protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) throws IOException {
        // Reset
        count = 0;

        // Create diagnostic contexts
        diagnosticContexts = CAT.getDiagnosticsByType(DbConstants.ResourceType.FileList).stream() //
                .map(this::createDiagnosticContext) //
                .collect(toList());

        // read log file
        readCsv(filePath);

        // Publish results
        diagnosticContexts.forEach(context -> {
            DiagnosticResult result = context.getResult();
            if (result != null) {
                resultConsumer.accept(result);
            }
        });
    }

    @Override
    public int getSize() {
        return count;
    }

    @SuppressWarnings("unchecked")
    private DiagnosticParseContext<FileDescription> createDiagnosticContext(DiagnosticSpecification diag) {
        return (DiagnosticParseContext<FileDescription>) diag.createContext(resource);
    }

    @Override
    protected void process(String[] split) {
        FileDescription current = FileDescription.parse(split);
        if (current != null) {
            count++;
            //feed log message to diags
            diagnosticContexts.forEach(action -> {
                try {
                    action.analyse(getRelativePath(), current);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
