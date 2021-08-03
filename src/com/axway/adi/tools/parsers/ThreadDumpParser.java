package com.axway.adi.tools.parsers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.DbConstants;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.DisturbMain.MAIN;
import static com.axway.adi.tools.parsers.ThreadDump.THREAD_NAME_HEADER;
import static javafx.scene.control.Alert.AlertType.WARNING;

public class ThreadDumpParser {

    private final List<ThreadDump> dumps = new ArrayList<>();
    private ThreadDump current = null;

    void addThread(String header) {
        current = new ThreadDump(header);
        dumps.add(current);
    }

    private void addStack(String line) {
        if (current != null) {
            current.addStack(line);
        }
    }

    public void parse(SupportCaseResource res, Consumer<DiagnosticResult> resultConsumer) throws IOException {
        Path path = Path.of(res.getAnalysisPath());
        if (Files.isRegularFile(path)) {
            analyzeThreadDumps(res, path, resultConsumer);
        }
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.walk(path, Integer.MAX_VALUE)) {
                stream.filter(Files::isRegularFile).limit(1).forEach(subPath -> analyzeThreadDumps(res, subPath, resultConsumer));
            }
        }
    }

    private void analyzeThreadDumps(SupportCaseResource res, Path redoLogFile, Consumer<DiagnosticResult> resultConsumer) {
        // Reset
        dumps.clear();
        current = null;
        // Read file
        readThreadDumps(redoLogFile);
        // aggregate dumps
        dumps.forEach(ThreadDump::aggregate);
        // run diagnostics
        MAIN.CAT.getDiagnosticsByType(DbConstants.ResourceType.ThreadDump).forEach(diag -> {
            DiagnosticParseContext<ThreadDump> context = (DiagnosticParseContext<ThreadDump>) diag.createContext(res);
            dumps.forEach(context);
            DiagnosticResult result = context.getResult();
            if (result != null) {
                resultConsumer.accept(result);
            }
        });
    }

    private void readThreadDumps(Path redoLogFile) {

        try (BufferedReader reader = new BufferedReader(new FileReader(redoLogFile.toFile()))) {
            String line;
            boolean header = true;
            boolean blankLine = false;
            boolean inThread = false;
            while ((line = reader.readLine()) != null) {
                boolean previousLineBlank = blankLine;
                blankLine = line.isBlank();
                if (blankLine) {
                    inThread = false;
                    continue;
                }
                line = line.trim();
                // Read header
                if (header) {
                    if (line.startsWith(THREAD_NAME_HEADER)) {
                        header = false;
                    }
                }
                if (previousLineBlank && line.startsWith(THREAD_NAME_HEADER)) {
                    inThread = true;
                    addThread(line);
                } else if (inThread) {
                    addStack(line);
                }
                //statistics.addError("Incomplete header");
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to read Redo Log file: " + redoLogFile + "\n" + ioException.getMessage());
        }
    }
}
