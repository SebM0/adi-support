package com.axway.adi.tools.disturb.diagnostics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;

import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.SupportArchive;

public class SupportArchiveAttributeError extends DiagnosticSpecification {
    private static final String DIAGNOSTIC_HEADER = "Scanned:";

    public SupportArchiveAttributeError() {
        id = "BUILTIN-SA-0001";
        name = "Attribute diagnostic";
        setLevel(DbConstants.Level.Error);
        setResourceType(SupportArchive);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class StatisticsContext extends DiagnosticParseContext<Path> {
        private DiagnosticResult result = null;

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, Path path) {
            try {
                Path attributeComputingDiagnosticPath = path.resolve("attributeComputingDiagnostic.txt");
                if (Files.isRegularFile(attributeComputingDiagnosticPath)) {
                    List<String> lines = Files.readAllLines(attributeComputingDiagnosticPath);
                    // Search for summary at end of file
                    //Scanned: 161 triggers, 161 indicators, 0 errors found
                    //Scanned: 4 triggers, 29 indicators, 25 errors found
                    for (int i = lines.size() - 1; i >= 0; i--) {
                        String line = lines.get(i);
                        if (line.startsWith(DIAGNOSTIC_HEADER)) {
                            String[] diagFragments = line.substring(DIAGNOSTIC_HEADER.length()).split(", ");
                            if (diagFragments.length > 0) {
                                // Last fragment is errors count
                                String[] split = diagFragments[diagFragments.length - 1].split(" ");
                                if (split.length > 0) {
                                    // Last fragment is errors count
                                    try {
                                        int errorCount = Integer.parseInt(split[0]);
                                        if (errorCount > 0) {
                                            result = buildResult();
                                            result.notes = line;
                                            result.addItem("report", String.join("\n", lines));
                                        }
                                    } catch (NumberFormatException e) {
                                        System.err.println("SupportArchiveParser: unexpected attributeComputingDiagnostic format, " + e.getMessage());
                                    }
                                }
                            }
                            return;
                        }
                    }
                } else {
                    System.err.println("SupportArchiveParser: 'attributeComputingDiagnostic.txt' do not exists");
                }
            } catch (IOException e) {
                System.err.println("SupportArchiveParser: " + e.getMessage());
            }
        }

        @Override
        public DiagnosticResult getResult() {
            return result;
        }
    }
}
