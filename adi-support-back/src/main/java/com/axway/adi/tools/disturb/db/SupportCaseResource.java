package com.axway.adi.tools.disturb.db;

import java.nio.file.Path;
import java.sql.Date;
import com.axway.adi.tools.disturb.db.DbConstants.ResourceType;
import com.axway.adi.tools.disturb.parsers.GlobalContext;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

@DbBind("SUPPORT_CASE_RESOURCE")
public class SupportCaseResource implements DbObject {
    public String name;
    @DbBind(foreign = true)
    public String parent_case;
    public String remote_path = "";
    public String local_path = "";
    public String local_ex_path = "";
    public Date update_time;
    public int type; //Unknown, support archive, thread dump(s), log(s), appx
    public int run_version;
    public int status; //new, downloaded, deployed, scan ok, scan failure
    public boolean ignored = false;
    private SupportCaseResource parent_resource;
    private GlobalContext globalContext;

    public SupportCaseResource() {
    }

    public SupportCaseResource(SupportCaseResource parent, Path localPath, ResourceType rt) {
        parent_resource = parent;
        parent_case = parent.parent_case;
        name = localPath.getFileName().toString();
        local_path = localPath.toString();
        setResourceType(rt);
    }

    public SupportCase getParent() {
        return CAT.getSupportCase(parent_case);
    }

    public String getFullName() {
        return parent_case + "." + name;
    }

    public ResourceType getResourceType() {
        if (type < 0 || type >= ResourceType.values().length)
            type = 0;
        return ResourceType.values()[type];
    }

    public final void setResourceType(ResourceType rt) {
        type = rt.ordinal();
    }

    public String getAnalysisPath() {
        if (local_ex_path != null && !local_ex_path.isEmpty()) {
            return local_ex_path;
        }
        if (local_path != null && !local_path.isEmpty()) {
            return local_path;
        }
        return Path.of(getParent().getLocalPath(), name).toString();
    }

    public SupportCaseResource getParentResource() {
        return parent_resource;
    }

    public GlobalContext getGlobalContext() {
        return globalContext == null && parent_resource != null ? parent_resource.getGlobalContext() : globalContext;
    }

    public void setGlobalContext(GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    @Override
    public String toString() {
        return name;
    }
}
