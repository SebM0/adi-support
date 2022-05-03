package com.axway.adi.tools.disturb.parsers.structures;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LogMessage {
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    public String date;
    public String domain;
    public String level;
    public String component;
    public String message;
    public JsonObject args;
    public List<String> dump;

    public Date parseDate() {
        try {
            return DATE_FORMATTER.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
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

    public static LogMessage parseJsonLog(String line) {
        //{"time": "2021-10-28 09:09:28,899", "metric": "runtimeSummary", "args": {"plans": "10/20/0", "abs:default": "16/16/9", "abs:indicator_computing": "16/16/3", "abs:cube_computing": "0/16/0", "abs:data_integration": "0/16/0", "abs:ws_data_integration": "0/16/0", "live": "10/10/27", "correction": "0/5/0", "h:com.systar.hvp.model.impl.PaymentStp": 4676, "h:Collector": 1, "h:Cube": 2711}}
        LogMessage msg = new LogMessage();

        JsonObject object = JsonParser.parseString(line).getAsJsonObject();

        msg.date = object.get("time").getAsString();
        msg.message = object.get("metric").getAsString();
        JsonElement args = object.get("args");
        msg.args = args != null ? args.getAsJsonObject() : new JsonObject();
        return msg;
    }
}
