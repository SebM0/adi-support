package com.axway.adi.tools.disturb.db;

import java.util.*;

public class DbConstants {
    public static final String ADI_JIRA_WRITER = "smoreno"; //""msa.rd.adi.robot";
    public static final String ADI_JIRA_WRITER_TOKEN = "3VK7EQ5LZMCAJRUCHNM76XHVZY2JM7ZHG7MBP2BUDUL2T6EBOXEX3RACG2UX4JJVQMLXLLYJL7YTGWJWETIDCUSFZ2CZAV2254PQ66BBCOHGGMUXE4VFOCP7WWEKCD6Y";

    public enum Status {
        New, InProgress, Done
    }

    public enum Level {
        Info, Warning, Error;

        public String toImage() {
            switch (this) {
                case Info:
                    return "ci-information";
                case Warning:
                    return "ci-warning-alt";
                case Error:
                    return "ci-error";
            }
            return null;
        }
    }

    public enum ResourceType {
        Unknown,
        SupportArchive,
        ThreadDump,
        Log,
        Appx,
        FileList;

        public static EnumSet<ResourceType> concrete() {
            return EnumSet.range(SupportArchive, FileList);
        }
    }

    private DbConstants() {

    }
}
