package com.axway.adi.tools.disturb.diagnostics;

import java.util.*;
import java.util.stream.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.FileDescription;
import com.axway.adi.tools.util.FileUtils;

public class FileListHuge extends DiagnosticSpecification {
    private static final long MAX_SIZE = 1 * FileUtils.GBYTES;

    public FileListHuge() {
        id = "BUILTIN-FL-0002";
        name = "Huge data files";
        description = "High volume in orphaned data folders: (> " + FileUtils.formatFileSize(MAX_SIZE) + ")";
        remediation = "Delete 'titanium-temporal/**/orphaned' folders";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.FileList);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new FileListStatisticsContext(this, res);
    }

    private static class FileListStatisticsContext extends DiagnosticParseContext<FileDescription> {
        private final List<FileDescription> hugeFiles = new ArrayList<>();
        long totalSize = 0;
        long maxSize = 0;

        protected FileListStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, FileDescription fd) {
            if (fd.isFile() && fd.path.startsWith("titanium-temporal/")) {
                totalSize += fd.size;
                maxSize = Math.max(maxSize, fd.size);
                if (fd.size > MAX_SIZE) {
                    hugeFiles.add(fd);
                }
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (!hugeFiles.isEmpty()) {
                DiagnosticResult result = buildResult();
                result.notes = FileUtils.formatFileSize(totalSize) + " in database folders\nDominators:\n\t";
                result.notes += hugeFiles.stream().sorted(Comparator.comparingLong(f -> -f.size)) //
                        .map(f -> f.path + " - " + FileUtils.formatFileSize(f.size)) //
                        .collect(Collectors.joining("\n\t"));
                return result;
            }
            return null;
        }
    }
}
