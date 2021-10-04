package com.axway.adi.tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;

public class ActivityParser {
    public static final String MEMORY_LOG_PATH = "var\\log\\internal-summary-memory.log";
    public static final String RUNTIME_LOG_PATH = "var\\log\\internal-runtime-activity.log";
    private static final int WAITING_NEW = 500;
    private static final int WAITING_EXISTING = 200;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean playing = new AtomicBoolean(true);

    public void start(ActivityController controller, Path runnerPath) {
        if (hasLogFile(runnerPath, MEMORY_LOG_PATH)) {
            new Thread(() -> {
                try {
                    launchParser(runnerPath.resolve(MEMORY_LOG_PATH), controller, running, playing);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        if (hasLogFile(runnerPath, RUNTIME_LOG_PATH)) {
            new Thread(() -> {
                try {
                    launchParser(runnerPath.resolve(RUNTIME_LOG_PATH), controller, running, playing);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private static boolean hasLogFile(Path runnerPath, String logSubPath) {
        try {
            Path path = runnerPath.resolve(logSubPath);
            return Files.isRegularFile(path);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static boolean hasLogFiles(Path runnerPath) {
        return hasLogFile(runnerPath, MEMORY_LOG_PATH) || hasLogFile(runnerPath, RUNTIME_LOG_PATH);
    }

    public void stop() {
        running.set(false);
    }

    public void setPlaying(boolean value) {
        playing.set(value);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    private static void launchParser(Path logFile, ActivityController controller, AtomicBoolean running, AtomicBoolean playing) throws IOException {
        try (FileInputStream fstream = new FileInputStream(logFile.toFile())) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while (running.get()) {
                if (!playing.get()) {
                    Thread.sleep(WAITING_NEW);
                    continue;
                }
                line = br.readLine();
                if (line == null) {
                    Thread.sleep(WAITING_NEW);
                } else {
                    if (WAITING_EXISTING > 0) {
                        Thread.sleep(WAITING_EXISTING);
                    }
                    controller.insertLine(line);
                }
            }
        } catch (InterruptedException e) {
            // stop
        }
    }
}
