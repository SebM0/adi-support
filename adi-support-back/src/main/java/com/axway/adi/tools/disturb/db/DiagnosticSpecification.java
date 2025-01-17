package com.axway.adi.tools.disturb.db;

import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;

@DbBind("DIAG_SPEC")
public class DiagnosticSpecification implements DbObject {
    @DbBind(primary = true)
    public String id;
    public String name;
    public String description;
    public String remediation;
    public String diagnostic; //script
    public int level;
    public int type; //Unknown, support archive, thread dump(s), log(s), appx

    private boolean custom = false;

    public boolean isCustom() {
        return custom;
    }

    public void setCustom() {
        this.custom = true;
    }

    public DbConstants.Level getLevel() {
        if (level < 0 || level >= DbConstants.Level.values().length)
            level = 0;
        return DbConstants.Level.values()[level];
    }

    public final void setLevel(DbConstants.Level level) {
        this.level = level.ordinal();
    }

    public DbConstants.ResourceType getResourceType() {
        if (type < 0 || type >= DbConstants.ResourceType.values().length)
            type = 0;
        return DbConstants.ResourceType.values()[type];
    }

    public final void setResourceType(DbConstants.ResourceType rt) {
        type = rt.ordinal();
    }

    public DiagnosticResult createResult(SupportCaseResource res) {
        DiagnosticResult result = new DiagnosticResult();
        result.parent_resource = res;
        result.parent_case = res.parent_case;
        result.spec = id;
        return result;
    }

    public DiagnosticParseContext<?> createContext(SupportCaseResource res) {
        return null;
    }
}
