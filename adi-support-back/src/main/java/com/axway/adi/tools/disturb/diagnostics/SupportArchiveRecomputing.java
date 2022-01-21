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
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;

import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.SupportArchive;

public class SupportArchiveRecomputing extends DiagnosticSpecification {

    public SupportArchiveRecomputing() {
        id = "BUILTIN-SA-0003";
        name = "Recomputing ratio";
        setLevel(DbConstants.Level.Warning);
        setResourceType(SupportArchive);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new StatisticsContext(this, res);
    }

    private static class IndicatorReport {
        String name;
        long liveCount = 0;
        long recomputingCount = 0;
        double getRatio() {
            return liveCount == 0 && recomputingCount == 0 ? 0.0 : (recomputingCount * 100.0) / ((double) (recomputingCount + liveCount));
        }
        public String toString() {
            return name + " : " + String.format("%.4f", getRatio()) + " (" + recomputingCount + " vs " + liveCount + ")";
        }
    }
    private static class StatisticsContext extends DiagnosticParseContext<Path> {
        private DiagnosticResult result = null;

        protected StatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, Path path) {
            try {
                Path indicatorReportPath = path.resolve("indicators-report.csv");
                if (Files.isRegularFile(indicatorReportPath)) {
                    Map<UUID, IndicatorReport> indicatorReports = new HashMap<>();
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
                            UUID column = UUID.fromString(getValue(split, "Indicator UUID"));
                            IndicatorReport report = indicatorReports.computeIfAbsent(column, c -> new IndicatorReport());
                            report.name = getValue(split, "Indicator entity name(s)") + "." + getValue(split, "Indicator name");
                            boolean recomputing = "recomputing".equals(getValue(split, "Recomputing"));
                            long count = Long.parseLong(getValue(split, "Count"));
                            if (recomputing) {
                                report.recomputingCount = count;
                            } else {
                                report.liveCount = count;
                            }
                        }
                    };
                    parser.readCsv(indicatorReportPath);
                    if (!indicatorReports.isEmpty()) {
                        long totalLive = indicatorReports.values().stream().mapToLong(r -> r.liveCount).sum();
                        long totalRecomputing = indicatorReports.values().stream().mapToLong(r -> r.recomputingCount).sum();
                        if (totalRecomputing > totalLive) {
                            result = buildResult();
                            double recomputingRatio = (totalRecomputing * 100.0) / ((double)(totalRecomputing + totalLive));
                            result.notes = "Recomputing ratio is big: " + String.format("%.4f", recomputingRatio) + " (" + totalRecomputing + " vs " + totalLive + ")\nTop recomputers:\n\t";
                            result.notes += indicatorReports.values().stream().sorted(Comparator.comparingDouble(f -> -f.getRatio())) //
                                    .limit(10) //
                                    .map(IndicatorReport::toString) //
                                    .collect(Collectors.joining("\n\t"));
                        }
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
