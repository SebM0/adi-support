package com.axway.adi.tools.parsers;

import java.io.*;
import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.util.DiagnosticCatalog.CAT;

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
        Parser parser = parseTestResource("node_stats.log", results);

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
        parseTestResource("node_many_replicas.log", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-LG-0002")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected many replicas result");
        Assert.assertTrue(result.get().notes.contains("19 replicas detected"), "expected many replicas in notes");
    }

    private Parser parseTestResource(String resourceName, List<DiagnosticResult> results) throws IOException {
        SupportCaseResource res = new SupportCaseResource();
        res.name = "Test resource";
        res.local_path = this.getClass().getResource(resourceName).getPath();
        if (res.local_path.startsWith("/") && res.local_path.charAt(2) == ':') {
            res.local_path = res.local_path.substring(1);
        }
        Parser parser = new LogParser(res);
        parser.parse(results::add);
        return parser;
    }
}
