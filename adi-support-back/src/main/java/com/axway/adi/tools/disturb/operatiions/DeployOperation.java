package com.axway.adi.tools.disturb.operatiions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.util.FileUtils;

public class DeployOperation extends Operation {
    public DeployOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        try {
            String localPath = getLocalPath();
            resource.local_ex_path = FileUtils.getDeploymentFolder(localPath);

            unzip(localPath);

            // 2nd pass if extracted file is a "tar"
            AtomicReference<IOException> excepCollector = new AtomicReference<>();
            try (Stream<Path> stream = Files.walk(Path.of(resource.local_ex_path), Integer.MAX_VALUE)) {
                stream.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().endsWith(".tar")).forEach(subPath -> {
                    try {
                        unzip(subPath.toString());
                        Files.delete(subPath);
                    } catch (IOException e) {
                        excepCollector.set(e);
                    }
                });
            }
            if (excepCollector.get() != null) {
                throw excepCollector.get();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void unzip(String localPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("7z x \"" + localPath + "\" -bd -y -o\"" + resource.local_ex_path + "\"").getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
