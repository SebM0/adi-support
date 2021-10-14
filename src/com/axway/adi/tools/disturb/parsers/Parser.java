package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

public abstract class Parser {
    protected final SupportCaseResource resource;

    protected Parser(SupportCaseResource resource) {
        this.resource = resource;
    }

    public void parse(Consumer<DiagnosticResult> resultConsumer) throws IOException {
        Path path = Path.of(resource.getAnalysisPath());
        if (Files.isRegularFile(path)) {
            parseFile(path, resultConsumer);
        }
        if (Files.isDirectory(path)) {
            AtomicReference<IOException> excepCollector = new AtomicReference<>();
            try (Stream<Path> stream = Files.walk(path, Integer.MAX_VALUE)) {
                filterFiles(stream.filter(Files::isRegularFile)).forEach(subPath -> {
                    try {
                        parseFile(subPath, resultConsumer);
                    } catch (IOException e) {
                        excepCollector.set(e);
                    }
                });
            }
            if (excepCollector.get() != null) {
                throw excepCollector.get();
            }
        }
    }

    protected Stream<Path> filterFiles(Stream<Path> stream) {
        return stream.limit(1);
    }

    protected abstract void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) throws IOException;

    public abstract int getSize();
}