package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.util.AlertHelper;

import static javafx.scene.control.Alert.AlertType.WARNING;

public abstract class CsvParser extends Parser {
    private final boolean m_skipHeader;
    private String m_separator = ",";
    private Map<String,Integer> m_headerNames = new HashMap<>();

    protected CsvParser(SupportCaseResource resource, boolean skipHeader) {
        super(resource);
        m_skipHeader = skipHeader;
    }

    public void readCsv(Path csvFile) {

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile.toFile()))) {
            boolean header = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    // detect separator
                    long commaCount = line.chars().filter(c -> c == ',').count();
                    long semiCount = line.chars().filter(c -> c == ';').count();
                    m_separator = commaCount > semiCount ? "," : ";";
                    String[] headerNames = line.split(m_separator);
                    for (int i=0; i< headerNames.length; i++) {
                        m_headerNames.put(headerNames[i], i);
                    }
                    if (m_skipHeader) {
                        continue; // skip header
                    }
                }
                line = line.trim();
                // Read line
                String[] split = line.split(m_separator);
                process(split);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            AlertHelper.show(WARNING, "Failed to read CSV file: " + csvFile + "\n" + ioException.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getValue(String[] split, String header) {
        Integer index = m_headerNames.get(header);
        if (index == null || index >= split.length) {
            return null;
        }
        return split[index];
    }

    protected abstract void process(String[] split);
}
