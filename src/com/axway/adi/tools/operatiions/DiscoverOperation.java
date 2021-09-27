package com.axway.adi.tools.operatiions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.util.db.DbConstants.ResourceType.*;

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
                        .map(path -> new SupportCaseResource(resource, path, Appx)) //
                        .forEach(sub -> {
                            driver.addOperation(new DeployOperation(sub));
                            driver.addOperation(new ScanOperation(sub));
                });
            }
            if (subFolderExists(THREADS_FOLDER)) {
                SupportCaseResource sub = new SupportCaseResource(resource, extendedPath.resolve(THREADS_FOLDER), ThreadDump);
                driver.addOperation(new ScanOperation(sub));
            }
            if (subFolderExists(FILE_LIST_FOLDER)) {
                SupportCaseResource sub = new SupportCaseResource(resource, extendedPath.resolve(FILE_LIST_FOLDER), FileList);
                driver.addOperation(new ScanOperation(sub));
            }
            if (subFolderExists(LOG_FOLDER)) {
                SupportCaseResource sub = new SupportCaseResource(resource, extendedPath.resolve(LOG_FOLDER), Log);
                driver.addOperation(new ScanOperation(sub));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            //AlertHelper.show(ERROR, e.getMessage());
        }
    }

    private boolean subFolderExists(String folder) {
        try {
            extendedPath.resolve(folder);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
