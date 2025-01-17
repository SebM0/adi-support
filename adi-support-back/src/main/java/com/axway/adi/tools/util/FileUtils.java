package com.axway.adi.tools.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FileUtils {
    public static final int KBYTES = 1024;
    public static final int MBYTES = 1024 * 1024;
    public static final int GBYTES = 1024 * 1024 * 1024;

    private FileUtils() {
    }

    public static String getDeploymentFolder(String filePath) throws FileNotFoundException {
        if (!Files.exists(Path.of(filePath))) {
            // File does not exist, fail
            throw new FileNotFoundException(filePath);
        }
        String testPath = filePath.toLowerCase();
        int extSep = testPath.lastIndexOf('.');
        if (extSep != -1) {
            if (testPath.substring(0, extSep).endsWith(".tar")) {
                extSep -= 4;
            }
            return filePath.substring(0, extSep);
        }
        return null;
    }

    public static String formatFileSize(long value) {
        if (value < KBYTES) {
            return value + " Bytes";
        } else if (value < MBYTES) {
            return formatSize(value, KBYTES, "KB");
        } else if (value < GBYTES) {
            return formatSize(value, MBYTES, "MB");
        } else {
            return formatSize(value, GBYTES, "GB");
        }
    }

    private static String formatSize(long fileSize, int unit, String unitString) {
        double value = Math.round((fileSize * 10) / (unit * 1.0d)) / 10.0d;
        DecimalFormat format = (value < 10) ? new DecimalFormat("#0.0") : new DecimalFormat("#0");
        return format.format(value) + " " + unitString;
    }

    private static final String EXTERNAL_GENERAL_ENTITIES_FEATURE = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES_FEATURE = "http://xml.org/sax/features/external-parameter-entities";

    private static DocumentBuilderFactory createDocumentFactory() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
        documentBuilderFactory.setFeature(EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
        documentBuilderFactory.setExpandEntityReferences(false);
        return documentBuilderFactory;
    }

    private static DocumentBuilder createDocumentBuilder() {
        try {
            DocumentBuilderFactory documentBuilderFactory = createDocumentFactory();
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document parseDocument(InputStream input) {
        try {
            return createDocumentBuilder().parse(new InputSource(input));
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isArchive(Object fileName) {
        String testName = fileName.toString().toLowerCase();
        return testName.endsWith(".zip") || testName.endsWith(".gz") || testName.endsWith(".tar") || testName.endsWith(".7z");

    }
}
