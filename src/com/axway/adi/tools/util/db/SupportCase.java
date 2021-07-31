package com.axway.adi.tools.util.db;

import java.util.*;
import com.axway.adi.tools.util.db.DbConstants.Status;

@DbBind("SUPPORT_CASE")
public class SupportCase implements DbObject {
    @DbBind(primary = true)
    public String id;
    public String summary;
    public String release;
    public String customer;
    public int run_version;
    public int status;

    private Map<String, SupportCaseResource> items = new HashMap<>();
    private String remotePath;
    private String localPath;

    public Status getStatus() {
        if (status < 0 || status >= Status.values().length)
            status = 0;
        return Status.values()[status];
    }

    public void addItem(SupportCaseResource item) {
        items.put(item.name, item);
    }

    public SupportCaseResource getItem(String name) {
        return items.get(name);
    }

    public Collection<SupportCaseResource> getItems() {
        return items.values();
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Override
    public String toString() {
        return id;
    }
}
