package com.axway.adi.tools.disturb.db;

@DbBind("DIAG_RESULT_ITEM")
public class DiagnosticResultItem implements DbObject {
    public String parent_result;
    public String item = "";
    public String notes = "";

    public DiagnosticResultItem() {}

    public DiagnosticResultItem(String item, String notes) {
        this.item = item;
        this.notes = notes;
    }

    @Override
    public String toString() {
        if (notes == null || notes.isEmpty())
            return item;
        return item + ": " + notes;
    }
}
