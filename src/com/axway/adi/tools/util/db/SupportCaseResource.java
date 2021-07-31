package com.axway.adi.tools.util.db;

import java.sql.Date;

import com.axway.adi.tools.util.db.DbConstants.ResourceType;

import static com.axway.adi.tools.DisturbMain.MAIN;

@DbBind("SUPPORT_CASE_ITEM")
public class SupportCaseResource implements DbObject {
    @DbBind(primary = true)
    public String name;
    public String parent_case;
    public String remote_path = "";
    public String local_path = "";
    public String local_ex_path = "";
    public Date update_time;
    public int type; //Unknown, support archive, thread dump(s), log(s), appx
    public int run_version;
    public int status; //new, downloaded, deployed, scan ok, scan failure
    public boolean ignored = false;

    public SupportCase getParent() {
        return MAIN.CAT.getSupportCase(parent_case);
    }

    public String getFullName() {
        return parent_case + "." + name;
    }

    public ResourceType getResourceType() {
        if (type < 0 || type >= ResourceType.values().length)
            type = 0;
        return ResourceType.values()[type];
    }

    @Override
    public String toString() {
        return name;
    }
}
