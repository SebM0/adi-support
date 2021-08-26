package com.axway.adi.tools.util.db;

import java.util.*;

import static com.axway.adi.tools.util.DiagnosticCatalog.CAT;

@DbBind("DIAG_RESULT")
public class DiagnosticResult implements DbObject {
    public String spec;
    public String parent_case;
    public String notes;

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

    public String toString() {
        return notes;
    }
}
