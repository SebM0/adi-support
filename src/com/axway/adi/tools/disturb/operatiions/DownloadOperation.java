package com.axway.adi.tools.disturb.operatiions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.util.AlertHelper;

import static javafx.scene.control.Alert.AlertType.ERROR;

public class DownloadOperation extends Operation {
    public DownloadOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        try {
            String localPath = getLocalPath();
            if (Files.exists(Path.of(localPath))) {
                // File already exists, skip
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Runtime.getRuntime().exec("curl --user rd-tnd-viewer:axwaydi2017 --silent " + resource.remote_path + " --output " + resource.local_path).getInputStream()))) {
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
