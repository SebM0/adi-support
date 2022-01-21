package com.axway.adi.tools.disturb.db;

import java.util.*;
import java.util.stream.*;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

@DbBind("DIAG_RESULT")
public class DiagnosticResult implements DbObject {
    public String spec;
    public String parent_case;
    public String notes;
    public SupportCaseResource parent_resource;

    private final List<DiagnosticResultItem> items = new ArrayList<>();

    public void addItem(String item, String notes) {
        items.add(new DiagnosticResultItem(item, notes));
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

    public String getNotes() {
        return notes;
    }

    public String getDetails() {
        return getItems().stream().map(DiagnosticResultItem::toString).collect(Collectors.joining("\n"));
    }

    private static final int LIMIT = 5;
    public String toString() {
        if (notes == null) {
            return "";
        }
        // limit to 5 lines
        int lineCount = 0;
        int pos = -1;
        while ((pos = notes.indexOf('\n', pos+1)) > 0 && lineCount < LIMIT) {
            lineCount++;
        }
        return pos > 0 ? notes.substring(0, pos) : notes;
    }
}
