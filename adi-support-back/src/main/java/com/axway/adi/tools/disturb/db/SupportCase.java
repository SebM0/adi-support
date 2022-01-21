package com.axway.adi.tools.disturb.db;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;
import com.axway.adi.tools.disturb.DisturbMain;

@DbBind("SUPPORT_CASE")
public class SupportCase implements DbObject {
    @DbBind(primary = true)
    public String id;
    public String summary;
    public String release;
    public String customer;
    public int run_version = 0;
    public Timestamp run_time;
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
        Path localPath;
        try {
            localPath = Path.of(DisturbMain.MAIN.getRootDirectory(), local_path);
        } catch (InvalidPathException e) {
            localPath = Path.of(local_path);
        }
        return localPath.toString();
    }

    public String getLastExecution() {
        return run_time != null ? run_time.toString() : "-";
    }

    public void onExecuted() {
        run_version++;
        run_time = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return id;
    }
}
