package com.axway.adi.tools.util.db;

import java.util.*;
import com.axway.adi.tools.util.db.DbConstants.Level;

@DbBind("DIAG_SPEC")
public class DiagnosticSpecification implements DbObject {
    @DbBind(primary = true)
    public String id;
    public String name;
    public String description;
    public String remediation;
    public String diagnostic; //script
    public int level;

    private boolean custom = false;

    public boolean isCustom() {
        return custom;
    }

    public void setCustom() {
        this.custom = true;
    }

    public Level getLevel() {
        if (level < 0 || level >= Level.values().length)
            level = 0;
        return Level.values()[level];
    }
}
