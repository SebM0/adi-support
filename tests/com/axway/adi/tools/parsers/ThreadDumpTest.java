package com.axway.adi.tools.parsers;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.axway.adi.tools.util.db.DiagnosticResult;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static com.axway.adi.tools.util.DiagnosticCatalog.CAT;

/**
 * Unit tests for the Thread dump.
 */
public class ThreadDumpTest {

    public ThreadDumpTest() {
        CAT.load();
    }

    @Test
    public void testThreadDumpStatistics() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("dump_threads_stats.txt", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Assert.assertEquals(parser.getSize(), 25, "expected number of threads");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-TD-0001")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected statistic result");
        Assert.assertTrue(result.get().notes.contains("25"), "expected number of thread in notes");
    }

    @Test
    public void testThreadDumpLowPerfOperator() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        parseTestResource("dump_threads_slow_ops.txt", results);

        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-TD-0002")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected result");
    }

    @Test
    public void testThreadDumpFlowedAbsorption() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        parseTestResource("dump_threads_flowed_absorption.txt", results);

        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-TD-0003")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected result");
    }

    private Parser parseTestResource(String resourceName, List<DiagnosticResult> results) throws IOException {
        SupportCaseResource res = new SupportCaseResource();
        res.name = "Test resource";
        res.local_path = this.getClass().getResource(resourceName).getPath();
        if (res.local_path.startsWith("/") && res.local_path.charAt(2) == ':') {
            res.local_path = res.local_path.substring(1);
        }
        Parser parser = new ThreadDumpParser(res);
        parser.parse(results::add);
        return parser;
    }
}
