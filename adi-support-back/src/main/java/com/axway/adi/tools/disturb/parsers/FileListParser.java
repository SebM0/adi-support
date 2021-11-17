package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.structures.FileDescription;
import com.axway.adi.tools.util.AlertHelper;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static java.util.stream.Collectors.*;
import static javafx.scene.control.Alert.AlertType.WARNING;

public class FileListParser extends Parser {
    int count = 0;
    List<DiagnosticParseContext<FileDescription>> diagnosticContexts;

    public FileListParser(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) throws IOException {
        // Reset
        count = 0;

        // Create diagnostic contexts
        diagnosticContexts = CAT.getDiagnosticsByType(DbConstants.ResourceType.FileList).stream() //
                .map(this::createDiagnosticContext) //
                .collect(toList());

        // read log file
        readFileList(filePath);

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
    private DiagnosticParseContext<FileDescription> createDiagnosticContext(DiagnosticSpecification diag) {
        return (DiagnosticParseContext<FileDescription>) diag.createContext(resource);
    }

    private void readFileList(Path redoLogFile) {

        try (BufferedReader reader = new BufferedReader(new FileReader(redoLogFile.toFile()))) {
            boolean header = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue; // skip header
                }
                line = line.trim();
                // Read header
                FileDescription current = FileDescription.parse(line);
                process(current);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to read file list: " + redoLogFile + "\n" + ioException.getMessage());
        }
    }

    private void process(FileDescription current) {
        if (current != null) {
            count++;
            //feed log message to diags
            diagnosticContexts.forEach(action -> action.analyse(getRelativePath(), current));
        }
    }
}
