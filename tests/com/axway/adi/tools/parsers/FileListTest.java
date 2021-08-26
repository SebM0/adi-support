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
public class FileListTest {

    public FileListTest() {
        CAT.load();
    }

    @Test
    public void testFileListOrphanedStatistics() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("file_list_data_orphaned.csv", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Assert.assertEquals(parser.getSize(), 283, "expected number of files");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-FL-0001")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected result");
        Assert.assertTrue(result.get().notes.contains("72 MB"), "expected orphaned volume in notes");
    }

    @Test
    public void testFileListOrphanedHuge() throws IOException {
        List<DiagnosticResult> results = new ArrayList<>();
        Parser parser = parseTestResource("file_list_data_huge.csv", results);

        Assert.assertFalse(results.isEmpty(), "expected results");
        Optional<DiagnosticResult> result = results.stream().filter(r -> r.spec.equals("BUILTIN-FL-0002")).findFirst();
        Assert.assertTrue(result.isPresent(), "expected result");
        Assert.assertTrue(result.get().notes.contains("5,3 GB"), "expected huge volume in notes");
    }

    private Parser parseTestResource(String resourceName, List<DiagnosticResult> results) throws IOException {
        SupportCaseResource res = new SupportCaseResource();
        res.name = "Test resource";
        res.local_path = this.getClass().getResource(resourceName).getPath();
        if (res.local_path.startsWith("/") && res.local_path.charAt(2) == ':') {
            res.local_path = res.local_path.substring(1);
        }
        Parser parser = new FileListParser(res);
        parser.parse(results::add);
        return parser;
    }
}
