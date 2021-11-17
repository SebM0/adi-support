package com.axway.adi.tools.disturb.parsers.structures;

import java.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LogMessage {
    public String date;
    public String domain;
    public String level;
    public String component;
    public String message;
    public JsonObject args;
    public List<String> dump;

    public void addDump(String line) {
        if (dump == null) {
            dump = new ArrayList<>();
        }
        dump.add(line);
    }
    public static boolean startsWithDate(String line) {
        if (line.length() < 24)
            return false;
        for (int i=0; i<24; i++) {
            char c = line.charAt(i);
            switch (i) {
                case 4:
                case 7:
                    if (c != '-') return false;
                    break;
                case 10:
                case 23:
                    if (c != ' ') return false;
                    break;
                case 13:
                case 16:
                    if (c != ':') return false;
                    break;
                case 19:
                    if (c != ',') return false;
                    break;
                default:
                    if (!Character.isDigit(c)) return false;
            }
        }
        return true;
    }

    public static LogMessage parseNodeLog(String line) {
        //2021-07-09 14:56:24,035 [Domain-calcium-403-databaseRedoLogServer] INFO electron.communicationServerManager - Channel registered {"args": {"type": "REDOLOG"}}
        LogMessage msg = new LogMessage();
        // Date: 2021-07-09 14:56:24,035
        int scanPos = 24;
        msg.date = line.substring(0, scanPos).trim();
        // Domain: [Domain-calcium-403-databaseRedoLogServer]
        int startPos = line.indexOf('[', scanPos);
        if (startPos != -1) {
            int endPos = line.indexOf(']', startPos+1);
            if (endPos != -1) {
                msg.domain = line.substring(startPos+1, endPos).trim();
                scanPos = endPos + 2;
            }
        }
        // Level: INFO
        startPos = line.indexOf(' ', scanPos);
        if (startPos != -1) {
            msg.level = line.substring(scanPos, startPos).trim();
            scanPos = startPos + 1;
        }
        // Component: electron.communicationServerManager -
        startPos = line.indexOf(" - ", scanPos);
        if (startPos != -1) {
            msg.component = line.substring(scanPos, startPos).trim();
            scanPos = startPos + 3;
        }
        // Args: {"args": {"type": "REDOLOG"}}
        startPos = line.lastIndexOf("{\"args\": ");
        if (startPos != -1) {
            msg.message = line.substring(scanPos, startPos).trim();
            String args = line.substring(startPos).trim();
            JsonObject object = JsonParser.parseString(args).getAsJsonObject();
            msg.args = object.get("args").getAsJsonObject();
        } else {
            msg.message = line.substring(scanPos).trim();
        }
        return msg;
    }

    public static LogMessage parseGCLog(String line) {
        // [2021-10-06T09:47:47.259+0200][12ms] Using Parallel
        // [2021-10-06T10:58:46.194+0200][4258947ms] GC(530) Marking Phase 4071.485ms
        LogMessage msg = new LogMessage();
        int scanPos = 1;
        // Date: [2021-10-06T10:58:46.194+0200]
        {
            int endPos = line.indexOf(']', scanPos);
            if (endPos != -1) {
                msg.date = line.substring(scanPos, endPos).trim();
                scanPos = endPos + 1;
            }
        }
        // Duration(stored in Domain): [4258947ms]
        int startPos = line.indexOf('[', scanPos);
        if (startPos != -1) {
            int endPos = line.indexOf(']', startPos + 1);
            if (endPos != -1) {
                msg.domain = line.substring(startPos + 1, endPos).trim();
                scanPos = endPos + 2;
            }
        }
        // Level: GC(530)
        if (line.substring(scanPos).startsWith("GC(")) {
            startPos = line.indexOf(')', scanPos);
            if (startPos != -1) {
                msg.level = line.substring(scanPos + "GC(".length(), startPos).trim();
                scanPos = startPos + 2;
            }
        }
        msg.message = line.substring(scanPos).trim();
        return msg;
    }
}
