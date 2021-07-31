package com.axway.adi.tools.util.db;

import java.sql.Date;

import static com.axway.adi.tools.DisturbMain.MAIN;

@DbBind("DIAG_RESULT_ITEM")
public class DiagnosticResultItem implements DbObject {
    public String parent_result;
    public String item = "";
    public String notes = "";

    @Override
    public String toString() {
        return item;
    }
}
