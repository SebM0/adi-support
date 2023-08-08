package com.axway.adi.tools.disturb.operatiions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

public class DiscoverOperation extends Operation {
    private static final String APPX_FOLDER = "Applications";
    private static final String THREADS_FOLDER = "thread-dumps";
    private static final String FILE_LIST_FOLDER = "file-list";
    private static final String LOG_FOLDER = "var/log";

    private Path extendedPath;

    public DiscoverOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        extendedPath = Path.of(resource.local_ex_path);
        try {
            if (!Files.exists(extendedPath)) {
                // File does not exist, fail
                throw new FileNotFoundException(resource.local_ex_path);
            }

            if (subFolderExists(APPX_FOLDER)) {
                Files.list(extendedPath.resolve(APPX_FOLDER)) //
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".appx")) //
                        .map(path -> new SupportCaseResource(resource, path, DbConstants.ResourceType.Appx)) //
                        .forEach(sub -> {
                            driver.addOperation(new DeployOperation(sub));
                            driver.addOperation(new ScanOperation(sub));
                });
            }
            if (subFolderExists(THREADS_FOLDER)) {
                SupportCaseResource sub = new SupportCaseResource(resource, extendedPath.resolve(THREADS_FOLDER), DbConstants.ResourceType.ThreadDump);
                driver.addOperation(new ScanOperation(sub));
            }
            if (subFolderExists(FILE_LIST_FOLDER)) {
                SupportCaseResource sub = new SupportCaseResource(resource, extendedPath.resolve(FILE_LIST_FOLDER), DbConstants.ResourceType.FileList);
                driver.addOperation(new ScanOperation(sub));
            }
            if (subFolderExists(LOG_FOLDER)) {
                SupportCaseResource sub = new SupportCaseResource(resource, extendedPath.resolve(LOG_FOLDER), DbConstants.ResourceType.Log);
                driver.addOperation(new ScanOperation(sub));
            }
            if (resource.getResourceType() == DbConstants.ResourceType.SupportArchive) {
                driver.addOperation(new ScanOperation(resource));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            //AlertHelper.show(ERROR, e.getMessage());
        }
    }

    private boolean subFolderExists(String folder) {
        try {
            Path path = extendedPath.resolve(folder);
            return Files.isDirectory(path);
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
