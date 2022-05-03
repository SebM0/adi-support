package com.axway.adi.tools.disturb.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.CsvParser;
import com.axway.adi.tools.disturb.parsers.GlobalContext;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.util.FileUtils;

import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.SupportArchive;

public class SupportArchiveColumnSize extends DiagnosticSpecification {

    public SupportArchiveColumnSize() {
        id = "BUILTIN-SA-0002";
        name = "Column size";
        description = "List 10 bigger store columns";
        setLevel(DbConstants.Level.Warning);
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
                Path columnSizePath = path.resolve("column-sizes.csv");
                if (Files.isRegularFile(columnSizePath)) {
                    Map<String,Long> sizeByStoreName = new HashMap<>();
                    CsvParser parser = new CsvParser(resource, true) {
                        @Override
                        protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) {
                        }

                        @Override
                        public int getSize() {
                            return 0;
                        }

                        @Override
                        protected void process(String[] split) {
                            String column = getValue(split, "Column Name");
                            long size = Long.parseLong(getValue(split, "SSTable data disk size"));
                            sizeByStoreName.put(column, size);
                        }
                    };
                    parser.readCsv(columnSizePath);
                    if (!sizeByStoreName.isEmpty()) {
                        long total = sizeByStoreName.values().stream().mapToLong(l -> l).sum();
                        GlobalContext globalContext = resource.getGlobalContext();
                        result = buildResult();
                        result.notes = FileUtils.formatFileSize(total) + " detected in " + sizeByStoreName.size() + " columns\n\t";
                        result.notes += sizeByStoreName.entrySet().stream().sorted(Comparator.comparingLong(f -> -f.getValue())) //
                                .limit(10) //
                                .map(f -> {
                                    String itemName = globalContext.getItemName(f.getKey());
                                    return itemName + " - " + FileUtils.formatFileSize(f.getValue());
                                }) //
                                .collect(Collectors.joining("\n\t"));
                    }
                }
            } catch (Exception e) {
                System.err.println("SupportArchiveParser: " + e.getMessage());
            }
        }

        @Override
        public DiagnosticResult getResult() {
            return result;
        }
    }
}
