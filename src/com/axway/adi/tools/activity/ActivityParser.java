package com.axway.adi.tools.activity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;

import static java.lang.Thread.sleep;

public class ActivityParser {
    public static final String MEMORY_LOG_PATH = "var\\log\\internal-summary-memory.log";
    public static final String RUNTIME_LOG_PATH = "var\\log\\internal-runtime-activity.log";
    private static final int WAITING_NEW = 500;
    private static final int WAITING_EXISTING = 200;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean playing = new AtomicBoolean(true);

    public void readActivity(ActivityController controller, Path runnerPath) {
        if (hasFile(runnerPath, MEMORY_LOG_PATH)) {
            new Thread(() -> {
                try {
                    launchParser(runnerPath.resolve(MEMORY_LOG_PATH), controller, true, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        if (hasFile(runnerPath, RUNTIME_LOG_PATH)) {
            new Thread(() -> {
                try {
                    launchParser(runnerPath.resolve(RUNTIME_LOG_PATH), controller, true, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private static boolean hasFile(Path runnerPath, String logSubPath) {
        try {
            Path path = runnerPath.resolve(logSubPath);
            return Files.isRegularFile(path);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static boolean hasActivityFiles(Path runnerPath) {
        return hasFile(runnerPath, MEMORY_LOG_PATH) || hasFile(runnerPath, RUNTIME_LOG_PATH);
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

    @SuppressWarnings("BusyWait")
    private void launchParser(Path logFile, ActivityHandler controller, boolean throttle, boolean stopAtEnd) throws IOException {
        try (FileInputStream fstream = new FileInputStream(logFile.toFile())) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while (running.get()) {
                if (!playing.get()) {
                    sleep(WAITING_NEW);
                    continue;
                }
                line = br.readLine();
                if (line == null) {
                    if (stopAtEnd) {
                        return;
                    } else {
                        sleep(WAITING_NEW);
                    }
                } else {
                    if (throttle && WAITING_EXISTING > 0) { // small pause to avoid UI overcrowd
                        sleep(WAITING_EXISTING);
                    }
                    controller.insertLine(line);
                }
            }
        } catch (InterruptedException e) {
            // stop
        }
    }
}
