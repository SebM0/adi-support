package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

/**
 * Unit tests for the Thread dump.
 */
public class LogTest {

    public LogTest() {
        CAT.load();
    }

    @Test
    public void testLogStatistics() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("node.log_stats", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Assert.assertEquals(parser.getSize(), 73, "expected number of files");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-LG-0001")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected log statistics result");
        Assert.assertTrue(result.get().notes.contains("Errors: 1"), "expected 1 error in notes");
        Assert.assertTrue(result.get().notes.contains("Warnings: 2"), "expected 2 warnings in notes");
    }

    @Test
    public void testLogManyReplicas() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        parseTestResource("node.log_many_replicas", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-LG-0002")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected many replicas result");
        Assert.assertTrue(result.get().notes.contains("19 replicas detected"), "expected many replicas in notes");
    }

    @Test
    public void testFullGC() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        parseTestResource("gc.log_fullpause", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-LG-0005")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected GC alert result");
        Assert.assertEquals(result.get().notes, "GC paused more than 50%, 6 times", "expected GC alert message in notes");
    }

    @Test
    public void testLogSlowCheckpoint() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        parseTestResource("node.log_slowCheckpoint", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-LG-0008")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected slow checkpoint result");
        Assert.assertEquals(result.get().notes, "1 / 1 slow checkpoints detected");
    }

    @Test
    public void testLogPolonium() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        parseTestResource("integration.log_polonium", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-LG-0009")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected polonium result");
        Assert.assertEquals(result.get().notes, "1 polonium uncaught exception detected");
    }

    @Test
    public void testLogPath() throws IOException {
        Assert.assertTrue(LogParser.NODE_LOG.test(Path.of("c:", "test", "node (1).log")), "should detect old pattern");
    }

    private Parser parseTestResource(String resourceName, List<DiagnosticResult> results) throws IOException {
        SupportCaseResource res = new SupportCaseResource();
        res.name = "Test resource";
        res.local_path = Objects.requireNonNull(this.getClass().getResource(resourceName)).getPath();
        if (res.local_path.startsWith("/") && res.local_path.charAt(2) == ':') {
            res.local_path = res.local_path.substring(1);
        }
        Parser parser = new LogParser(res);
        parser.parse(results::add);
        return parser;
    }
}
