package com.axway.adi.tools.disturb.db;

import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.DisturbMain;

@DbBind("SUPPORT_CASE")
public class SupportCase implements DbObject {
    @DbBind(primary = true)
    public String id;
    public String summary;
    public String release;
    public String customer;
    public int run_version;
    public int status;
    public String remote_path = "";
    public String local_path = "";

    private Map<String, SupportCaseResource> resources = new HashMap<>();

    public DbConstants.Status getStatus() {
        if (status < 0 || status >= DbConstants.Status.values().length)
            status = 0;
        return DbConstants.Status.values()[status];
    }

    public void addItem(SupportCaseResource item) {
        resources.put(item.name, item);
        item.parent_case = id;
    }

    public SupportCaseResource getItem(String name) {
        return resources.get(name);
    }

    public Collection<SupportCaseResource> getResources() {
        return resources.values();
    }

    public String getLocalPath() {
        if (local_path == null || local_path.isEmpty())
            return "";
        return Path.of(DisturbMain.MAIN.getRootDirectory(), local_path).toString();
    }

    @Override
    public String toString() {
        return id;
    }
}
