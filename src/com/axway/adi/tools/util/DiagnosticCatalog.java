package com.axway.adi.tools.util;

import java.util.*;
import com.axway.adi.tools.util.db.DbConstants.Status;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCase;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.DisturbMain.MAIN;
import static java.util.stream.Collectors.*;

public class DiagnosticCatalog {
    public static final int VERSION = 1;
    private Map<String, SupportCase> supportCases;
    private Map<String, DiagnosticSpecification> specifications = new HashMap<>();

    public void load() {
        // Support case
        supportCases = MAIN.DB.select(SupportCase.class).stream().collect(toMap(c -> c.id, c -> c));
        // Support case resources
        List<SupportCaseResource> supportCaseResources = MAIN.DB.select(SupportCaseResource.class);
        supportCaseResources.forEach(item -> {
            SupportCase supportCase = supportCases.get(item.parent_case);
            if (supportCase != null) {
                supportCase.addItem(item);
            } else {
                //todo delete orphan item
            }
        });
        // custom diagnostics
        Map<String, DiagnosticSpecification> customSpecifications = MAIN.DB.select(DiagnosticSpecification.class).stream().collect(toMap(c -> c.id, c -> c));
        customSpecifications.values().forEach(DiagnosticSpecification::setCustom);
        specifications.putAll(customSpecifications);
    }

    public List<SupportCase> getSupportCasesByStatus(Status status) {
        return supportCases.values().stream().filter(sc -> sc.getStatus() == status).collect(toList());
    }

    public SupportCase getSupportCase(String id) {
        return supportCases.get(id);
    }

    public DiagnosticSpecification getSpecification(String id) {
        return specifications.get(id);
    }
}
