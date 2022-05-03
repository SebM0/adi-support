package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.SupportCaseResource;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;

/**
 * Unit tests for the Thread dump.
 */
public class AppxTest {

    public AppxTest() {
        CAT.load();
    }

    @Test
    public void testAppxStatistics() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("application.xml", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Assert.assertEquals(parser.getSize(), 9, "expected number of items");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-AP-0001")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected appx statistics result");
        Assert.assertTrue(result.get().notes.contains("Entities: 2"), "expected 2 entities in notes");
        Assert.assertTrue(result.get().notes.contains("Attributes: 5"), "expected 5 attributes in notes");
        Assert.assertTrue(result.get().getItems().stream().anyMatch(item -> item.item.equals("BaselinePointDecimal") && item.notes.equals("1")), "expected 1 baseline in notes");
    }

    @Test
    public void testAppxObsolete() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("application_obsolete.xml", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-AP-0002")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected appx obsolete result");
        Assert.assertEquals(result.get().notes, "Baseline: [[EntityB].BaselineNew (Baseline)]");
    }

    @Test
    public void testAppxNoneObsolete() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("application.xml", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-AP-0002")).findFirst();
        Assert.assertFalse(result.isPresent(), "expected NO appx obsolete result");
    }

    @Test
    public void testAppxPurge() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("application.xml", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Assert.assertEquals(parser.getSize(), 9, "expected number of items");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-AP-0003")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected appx statistics result");
        Assert.assertTrue(result.get().notes.contains("Unlimited"), "expected unlimited setting in notes");
        Assert.assertTrue(result.get().notes.contains("months|5"), "expected 5 months setting in notes");
    }

    private Parser parseTestResource(String resourceName, List<DiagnosticResult> results) throws IOException {
        SupportCaseResource res = new SupportCaseResource();
        res.name = "Test resource";
        res.local_path = this.getClass().getResource(resourceName).getPath();
        if (res.local_path.startsWith("/") && res.local_path.charAt(2) == ':') {
            res.local_path = res.local_path.substring(1);
        }
        Parser parser = new ApplicationParser(res);
        parser.parse(results::add);
        return parser;
    }
}
