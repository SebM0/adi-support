package com.axway.adi.tools.disturb.diagnostics;

import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.FileDescription;

public class FileListLVStorageNotActivated extends DiagnosticSpecification {

    public FileListLVStorageNotActivated() {
        id = "BUILTIN-FL-0003";
        name = "LV storage not activated";
        description = "Low Volume storage not activated. It cause performance issues at startup and model changes";
        remediation = "Activate it with property 'com.systar.titanium.lowVolumeColumn=forceOn'";
        setLevel(DbConstants.Level.Warning);
        setResourceType(DbConstants.ResourceType.FileList);
    }

    @Override
    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return new FileListStatisticsContext(this, res);
    }

    private static class FileListStatisticsContext extends DiagnosticParseContext<FileDescription> {
        private int lvCount = 0;

        protected FileListStatisticsContext(DiagnosticSpecification specification, SupportCaseResource resource) {
            super(specification, resource);
        }

        @Override
        public void analyse(String resFile, FileDescription fd) {
            if (fd.isFile() && fd.path.endsWith("lvmemtable.bin")) {
                lvCount++;
            }
        }

        @Override
        public DiagnosticResult getResult() {
            if (lvCount == 0) {
                DiagnosticResult result = buildResult();
                result.notes = "No LV mem table detected";
                return result;
            }
            return null;
        }
    }
}
