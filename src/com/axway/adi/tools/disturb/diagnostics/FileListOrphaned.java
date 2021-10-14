package com.axway.adi.tools.disturb.diagnostics;

import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.FileDescription;

import static com.axway.adi.tools.disturb.db.DbConstants.Level.Warning;
import static com.axway.adi.tools.disturb.db.DbConstants.ResourceType.FileList;
import static com.axway.adi.tools.util.FileUtils.*;

public class FileListOrphaned extends DiagnosticSpecification {
    private static final long MAX_SIZE = 10 * MBYTES;

    public FileListOrphaned() {
        id = "BUILTIN-FL-0001";
        name = "Orphaned folder";
        description = "High volume in orphaned data folders: (> " + formatFileSize(MAX_SIZE) + ")";
        remediation = "Delete 'titanium-temporal/**/orphaned' folders";
        setLevel(Warning);
        setResourceType(FileList);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new FileListStatisticsContext(this, res);
    }

    private static class FileListStatisticsContext extends DiagnosticParseContext<FileDescription> {
        private long totalSize = 0;

        protected FileListStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void accept(FileDescription fd) {
            if (fd.isDirectory() && fd.path.endsWith("/orphaned")) {
                totalSize += fd.size;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (totalSize > MAX_SIZE) {
                DiagnosticResult result = buildResult();
                result.notes = formatFileSize(totalSize) + " in orphaned folders";
                return result;
            }
            return null;
        }
    }
}
