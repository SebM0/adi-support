package com.axway.adi.tools.operatiions;

import com.axway.adi.tools.parsers.ThreadDumpParser;
import com.axway.adi.tools.util.AlertHelper;
import com.axway.adi.tools.util.db.SupportCaseResource;

import static javafx.scene.control.Alert.AlertType.*;

public class ScanOperation extends Operation {

    public ScanOperation(SupportCaseResource resource) {
        super(resource);
    }

    @Override
    public void run() {
        try {
            switch (resource.getResourceType()) {
                case ThreadDump:
                    ThreadDumpParser parser = new ThreadDumpParser(resource);
                    parser.parse(driver::addResult);
                    break;
                default:
                    AlertHelper.show(INFORMATION, resource.getResourceType().name() + " not handled yet");
            }
        } catch (Exception e) {
            AlertHelper.show(ERROR, e.getMessage());
        }
    }
}
