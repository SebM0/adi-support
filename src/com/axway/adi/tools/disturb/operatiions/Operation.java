package com.axway.adi.tools.disturb.operatiions;

import java.nio.file.Path;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

public abstract class Operation implements Runnable {
    protected final SupportCaseResource resource;
    protected OperationDriver driver;

    protected Operation(SupportCaseResource resource) {
        this.resource = resource;
    }

    void setDriver(OperationDriver driver) {
        this.driver = driver;
    }

    public String getName() {
        String className = this.getClass().getSimpleName();
        return className.replace("Operation", "");
    }

    public String getFullName() {
        return getName() + " " + resource.getFullName();
    }

    protected String getLocalPath() {
        if (resource.local_path == null || resource.local_path.isEmpty()) {
            // build local path
            resource.local_path = Path.of(resource.getParent().getLocalPath(), resource.name).toString();
        }
        return resource.local_path;
    }
}
