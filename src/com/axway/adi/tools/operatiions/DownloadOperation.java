package com.axway.adi.tools.operatiions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static javafx.scene.control.Alert.AlertType.ERROR;

public class DownloadOperation extends Operation {
    public DownloadOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        if (resource.local_path == null || resource.local_path.isEmpty()) {
            // build local path
            resource.local_path = Path.of(resource.getParent().getLocalPath(), resource.name).toString();
        }
        if (Files.exists(Path.of(resource.local_path))) {
            // File already exists, skip
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Runtime.getRuntime().exec("curl --user rd-tnd-viewer:axwaydi2017 --silent " + resource.remote_path + " --output " + resource.local_path).getInputStream()))) {
            String line;
            while ((line =reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            AlertHelper.show(ERROR, e.getMessage());
            throw new RuntimeException(e);  // TODO handle exception
        }
    }
}
