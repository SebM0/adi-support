package com.axway.adi.tools.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.DbConstants;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.DiagnosticSpecification;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.parsers.ThreadDump.THREAD_NAME_HEADER;
import static com.axway.adi.tools.util.DiagnosticCatalog.CAT;
import static javafx.scene.control.Alert.AlertType.WARNING;

public class ThreadDumpParser extends Parser {
    private final List<ThreadDump> dumps = new ArrayList<>();
    private ThreadDump current = null;

    public ThreadDumpParser(SupportCaseResource resource) {
        super(resource);
    }

    public List<ThreadDump> getDumps() {
        return dumps;
    }

    void addThread(String header) {
        current = new ThreadDump(header);
        dumps.add(current);
    }

    private void addStack(String line) {
        if (current != null) {
            current.addStack(line);
        }
    }

    @Override
    protected void parseFile(Path redoLogFile, Consumer<DiagnosticResult> resultConsumer) {
        // Reset
        dumps.clear();
        current = null;
        // Read file
        readThreadDumps(redoLogFile);
        // aggregate dumps
        dumps.forEach(ThreadDump::aggregate);
        // run diagnostics
        CAT.getDiagnosticsByType(DbConstants.ResourceType.ThreadDump).forEach(diag -> {
            DiagnosticParseContext<ThreadDump> context = createDiagnosticContext(diag);
            dumps.forEach(context);
            DiagnosticResult result = context.getResult();
            if (result != null) {
                resultConsumer.accept(result);
            }
        });
    }

    @Override
    public int getSize() {
        return getDumps().size();
    }

    private DiagnosticParseContext<ThreadDump> createDiagnosticContext(DiagnosticSpecification diag) {
        if (diag.isCustom()) {
            return new ThreadDumpContext(diag, resource);
        } else {
            return (DiagnosticParseContext<ThreadDump>) diag.createContext(resource);
        }
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
