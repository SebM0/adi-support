package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.LogMessage;
import com.axway.adi.tools.util.AlertHelper;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static java.util.stream.Collectors.*;
import static javafx.scene.control.Alert.AlertType.WARNING;

public class LogParser extends Parser {
    public static final Predicate<Path> NODE_LOG = f -> f.getFileName().toString().toLowerCase().startsWith("node.log");
    public static final Predicate<Path> GC_LOG = f -> f.getFileName().toString().toLowerCase().startsWith("gc.log");
    public static final Predicate<Path> ACTIVITY_LOG = f -> f.getFileName().toString().toLowerCase().startsWith("internal-runtime-activity.log");
    public static final Predicate<Path> MEMORY_LOG = f -> f.getFileName().toString().toLowerCase().startsWith("internal-summary-memory.log");

    LogMessage currentMessage = null;
    int count = 0;
    List<DiagnosticParseContext<LogMessage>> diagnosticContexts;

    public LogParser(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    protected Stream<Path> filterFiles(Stream<Path> stream) {
        return stream.filter(f -> f.getFileName().toString().toLowerCase().contains(".log"));
    }

    @Override
    protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) {
        // Reset
        currentMessage = null;
        count = 0;

        // Create diagnostic contexts
        diagnosticContexts = CAT.getDiagnosticsByType(DbConstants.ResourceType.Log).stream() //
                .map(this::createDiagnosticContext) //
                .filter(c -> c.filter(filePath)) //
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

    private void readLogs(Path filePath) {
        if (NODE_LOG.test(filePath)) {
            readNodeLogs(filePath);
        } else if (GC_LOG.test(filePath)) {
            readGCLogs(filePath);
        } else if (MEMORY_LOG.test(filePath) || ACTIVITY_LOG.test(filePath)) {
            readSummaryLogs(filePath);
        } else {
            System.err.println("Unsupported log file " + filePath);
        }
    }

    private void readNodeLogs(Path filePath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Read header
                if (LogMessage.startsWithDate(line)) {
                    processLogMessage();
                    currentMessage = LogMessage.parseNodeLog(line);
                } else if (currentMessage != null) {
                    currentMessage.addDump(line);
                }
            }
            processLogMessage();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to Log file: " + filePath + "\n" + ioException.getMessage());
        }
    }

    private void readGCLogs(Path filePath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Read header
                if (line.startsWith("[")) {
                    currentMessage = LogMessage.parseGCLog(line);
                    processLogMessage();
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to Log file: " + filePath + "\n" + ioException.getMessage());
        }
    }

    private void readSummaryLogs(Path filePath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Read header
                currentMessage = LogMessage.parseJsonLog(line);
                processLogMessage();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to Log file: " + filePath + "\n" + ioException.getMessage());
        }
    }

    private void processLogMessage() {
        if (currentMessage != null) {
            count++;
            //feed log message to diags
            diagnosticContexts.forEach(action -> action.analyse(getRelativePath(), currentMessage));
        }
    }
}
