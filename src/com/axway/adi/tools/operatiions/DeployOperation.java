package com.axway.adi.tools.operatiions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static javafx.scene.control.Alert.AlertType.ERROR;

public class DeployOperation extends Operation {
    public DeployOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        try {
            String localPath = getLocalPath();
            if (!Files.exists(Path.of(localPath))) {
                // File does not exist, fail
                throw new FileNotFoundException(resource.local_path);
            }
            String testPath = localPath.toLowerCase();
            int extSep = testPath.lastIndexOf('.');
            if (extSep != -1) {
                resource.local_ex_path = localPath.substring(0, extSep);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Runtime.getRuntime().exec("7z x \"" + localPath + "\" -bd -y -o\"" + resource.local_ex_path + "\"").getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
    }
}
