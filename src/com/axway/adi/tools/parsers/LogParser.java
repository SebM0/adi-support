package com.axway.adi.tools.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.DbConstants;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.util.DiagnosticCatalog.CAT;
import static java.util.stream.Collectors.*;
import static javafx.scene.control.Alert.AlertType.WARNING;

public class LogParser extends Parser {
    LogMessage current = null;
    int count = 0;
    List<DiagnosticParseContext<LogMessage>> diagnosticContexts;

    public LogParser(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    protected Stream<Path> filterFiles(Stream<Path> stream) {
        List<Path> files = stream.collect(toList());
        if (files.size() <= 3) {
            return files.stream();
        }
        return files.stream().filter(f -> f.getFileName().toString().toLowerCase().startsWith("node.log"));
    }

    @Override
    protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) throws IOException {
        // Reset
        current = null;
        count = 0;

        // Create diagnostic contexts
        diagnosticContexts = CAT.getDiagnosticsByType(DbConstants.ResourceType.Log).stream() //
                .map(this::createDiagnosticContext) //
                .collect(toList());

        // read log file
        readLogs(filePath);

        // Publish results
        diagnosticContexts.forEach(context -> {
            DiagnosticResult result = context.getResult();
            if (result != null) {
                resultConsumer.accept(result);
            }
        });
    }

    @Override
    public int getSize() {
        return count;
    }

    @SuppressWarnings("unchecked")
    private DiagnosticParseContext<LogMessage> createDiagnosticContext(DiagnosticSpecification diag) {
        return (DiagnosticParseContext<LogMessage>) diag.createContext(resource);
    }

    private void readLogs(Path redoLogFile) {

        try (BufferedReader reader = new BufferedReader(new FileReader(redoLogFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Read header
                if (LogMessage.startsWithDate(line)) {
                    processLogMessage();
                    current = LogMessage.parse(line);
                } else if (current != null) {
                    current.addDump(line);
                }
            }
            processLogMessage();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to Log file: " + redoLogFile + "\n" + ioException.getMessage());
        }
    }

    private void processLogMessage() {
        if (current != null) {
            count++;
            //feed log message to diags
            diagnosticContexts.forEach(action -> action.accept(current));
        }
    }
}