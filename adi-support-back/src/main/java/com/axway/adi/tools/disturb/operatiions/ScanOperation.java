package com.axway.adi.tools.disturb.operatiions;

import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.ApplicationParser;
import com.axway.adi.tools.disturb.parsers.FileListParser;
import com.axway.adi.tools.disturb.parsers.LogParser;
import com.axway.adi.tools.disturb.parsers.SupportArchiveParser;
import com.axway.adi.tools.disturb.parsers.ThreadDumpParser;
import com.axway.adi.tools.util.AlertHelper;

import static javafx.scene.control.Alert.AlertType.ERROR;

public class ScanOperation extends Operation {

    public ScanOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        try {
            switch (resource.getResourceType()) {
                case ThreadDump: {
                    ThreadDumpParser parser = new ThreadDumpParser(resource);
                    parser.parse(driver::addResult);
                    break;
                }
                case Log: {
                    LogParser parser = new LogParser(resource);
                    parser.parse(driver::addResult);
                    break;
                }
                case FileList: {
                    FileListParser parser = new FileListParser(resource);
                    parser.parse(driver::addResult);
                    break;
                }
                case Appx: {
                    ApplicationParser parser = new ApplicationParser(resource);
                    parser.parse(driver::addResult);
                    break;
                }
                case SupportArchive: {
                    SupportArchiveParser parser = new SupportArchiveParser(resource);
                    parser.parse(driver::addResult);
                    break;
                }
                default:
                    System.err.println(resource.getResourceType().name() + " not handled yet");
            }
        } catch (Exception e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
    }
}
