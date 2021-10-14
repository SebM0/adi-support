package com.axway.adi.tools.disturb;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbConstants.ResourceType;
import com.axway.adi.tools.disturb.db.DbConstants.Status;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCase;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.diagnostics.AppxObsoleteTypes;
import com.axway.adi.tools.disturb.diagnostics.AppxPurge;
import com.axway.adi.tools.disturb.diagnostics.AppxStatistics;
import com.axway.adi.tools.disturb.diagnostics.FileListHuge;
import com.axway.adi.tools.disturb.diagnostics.FileListLVStorageNotActivated;
import com.axway.adi.tools.disturb.diagnostics.FileListOrphaned;
import com.axway.adi.tools.disturb.diagnostics.LogGarbageCollectorAlert;
import com.axway.adi.tools.disturb.diagnostics.LogLVStorageNotActivated;
import com.axway.adi.tools.disturb.diagnostics.LogManyReplicas;
import com.axway.adi.tools.disturb.diagnostics.LogManyZooKeeperClients;
import com.axway.adi.tools.disturb.diagnostics.LogStatistics;
import com.axway.adi.tools.disturb.diagnostics.ThreadDumpFlowedAbsorption;
import com.axway.adi.tools.disturb.diagnostics.ThreadDumpLowPerformancePlanOperators;
import com.axway.adi.tools.disturb.diagnostics.ThreadDumpStatistics;

import static com.axway.adi.tools.disturb.DiagnosticPersistence.DB;
import static com.axway.adi.tools.disturb.DisturbMain.MAIN;
import static com.axway.adi.tools.disturb.db.DbConstants.Status.InProgress;
import static java.util.stream.Collectors.*;

public class DiagnosticCatalog {
    public static final DiagnosticCatalog CAT = new DiagnosticCatalog();
    private Map<String, SupportCase> supportCases = null;
    private final Map<String, DiagnosticSpecification> specifications = new HashMap<>();

    public void load() {
        // clear
        if (supportCases != null)
            supportCases.clear();
        specifications.clear();
        // load from DB
        if (DB != null) {
            // Support case
            supportCases = DB.select(SupportCase.class).stream().collect(toMap(c -> c.id, c -> c));
            // Support case resources
            List<SupportCaseResource> supportCaseResources = DB.select(SupportCaseResource.class);
            supportCaseResources.forEach(item -> {
                SupportCase supportCase = supportCases.get(item.parent_case);
                if (supportCase != null) {
                    supportCase.addItem(item);
                } else {
                    //todo delete orphan item
                }
            });
            // custom diagnostics
            Map<String, DiagnosticSpecification> customSpecifications = DB.select(DiagnosticSpecification.class).stream().collect(toMap(c -> c.id, c -> c));
            customSpecifications.values().forEach(DiagnosticSpecification::setCustom);
            specifications.putAll(customSpecifications);
        } else if (MAIN != null) {
            // Offline mode
            String rootDirectory = MAIN.getRootDirectory();
            if (rootDirectory != null && !rootDirectory.isEmpty()) {
                Path rootPath = Path.of(rootDirectory);
                try {
                    supportCases = Files.list(rootPath) //
                            .filter(path -> path.getFileName().toString().toUpperCase().startsWith("TORNADO-")) //
                            .map(path -> {
                                SupportCase supportCase = new SupportCase();
                                supportCase.local_path = supportCase.id = path.getFileName().toString();
                                supportCase.status = InProgress.ordinal();
                                return supportCase;
                            }) //
                            .collect(toMap(c -> c.id, c -> c));
                } catch (IOException e) {
                    //
                }
            }
        }
        // built-in
        addDiagnostic(new ThreadDumpStatistics());
        addDiagnostic(new ThreadDumpLowPerformancePlanOperators());
        addDiagnostic(new ThreadDumpFlowedAbsorption());
        addDiagnostic(new LogStatistics());
        addDiagnostic(new LogManyReplicas());
        addDiagnostic(new LogManyZooKeeperClients());
        addDiagnostic(new LogLVStorageNotActivated());
        addDiagnostic(new LogGarbageCollectorAlert());
        addDiagnostic(new FileListOrphaned());
        addDiagnostic(new FileListHuge());
        addDiagnostic(new FileListLVStorageNotActivated());
        addDiagnostic(new AppxStatistics());
        addDiagnostic(new AppxObsoleteTypes());
        addDiagnostic(new AppxPurge());
    }

    public List<SupportCase> getSupportCasesByStatus(Status status) {
        return supportCases.values().stream().filter(sc -> sc.getStatus() == status).collect(toList());
    }

    public SupportCase createSupportCases(String id) {
        SupportCase supportCase = new SupportCase();
        supportCase.id = id;
        supportCase.status = InProgress.ordinal();
        return supportCase;
    }

    public SupportCase getSupportCase(String id) {
        return supportCases.get(id);
    }

    public void deleteSupportCase(SupportCase sc) {
        supportCases.remove(sc.id);
        if (DB != null) {
            DB.delete(sc);
        }
    }

    public DiagnosticSpecification getDiagnostic(String id) {
        return specifications.get(id);
    }

    public void addDiagnostic(DiagnosticSpecification diag) {
        specifications.put(diag.id, diag);
    }

    public List<DiagnosticSpecification> getDiagnosticsByType(ResourceType resourceType) {
        return specifications.values().stream().filter(diag -> diag.getResourceType() == resourceType).collect(toList());
    }
}