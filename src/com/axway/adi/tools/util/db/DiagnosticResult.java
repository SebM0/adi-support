package com.axway.adi.tools.util.db;

import java.util.*;

import static com.axway.adi.tools.DisturbMain.MAIN;

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
        return MAIN.CAT.getSupportCase(parent_case);
    }

    public DiagnosticSpecification getSpec() {
        return MAIN.CAT.getSpecification(parent_case);
    }
}