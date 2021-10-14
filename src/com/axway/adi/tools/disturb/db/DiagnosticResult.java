package com.axway.adi.tools.disturb.db;

import java.util.*;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

@DbBind("DIAG_RESULT")
public class DiagnosticResult implements DbObject {
    public String spec;
    public String parent_case;
    public String notes;
    public SupportCaseResource parent_resource;

    private List<DiagnosticResultItem> items = new ArrayList<>();

    public void addItem(DiagnosticResultItem item) {
        items.add(item);
    }

    public Collection<DiagnosticResultItem> getItems() {
        return items;
    }

    public SupportCase getParent() {
        return CAT.getSupportCase(parent_case);
    }

    public DiagnosticSpecification getSpec() {
        return CAT.getDiagnostic(spec);
    }

    public Integer getLevel() {
        DiagnosticSpecification spec = getSpec();
        return spec != null ? spec.level : 0;
    }

    public String getSpecName() {
        DiagnosticSpecification specification = getSpec();
        return specification != null ? specification.name : spec;
    }

    public String getParentResource() {
        SupportCaseResource res = parent_resource;
        while (res != null && res.getParentResource() != null) {
            res = res.getParentResource();
        }
        return res != null ? res.name : "";
    }

    public String toString() {
        return notes;
    }
}