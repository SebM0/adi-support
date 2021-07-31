package com.axway.adi.tools.operatiions;

import com.axway.adi.tools.util.db.SupportCaseResource;

public abstract class Operation implements Runnable {
    protected final SupportCaseResource resource;

    protected Operation(SupportCaseResource resource) {
        this.resource = resource;
    }

    public String getName() {
        String className = this.getClass().getSimpleName();
        return className.replace("Operation", "");
    }

    public String getFullName() {
        return getName() + " " + resource.getFullName();
    }
}
