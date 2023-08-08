package com.axway.adi.tools.disturb.db;

import java.util.*;

public class DbConstants {
    public static final String ADI_JIRA_WRITER = "smoreno"; //""msa.rd.adi.robot";
    public static final String ADI_JIRA_WRITER_TOKEN = "VOABDBK7WRRW4ZG2SFGEOTRSSVWAKADIZLVNZC32B6HHIAH5RW7EZK4SQKTYUDCUYOUYEWMLJ5O2UWMPBMSFEVHU4KBLZ3F3VUXCUPJH7WLPJFN4ONJWXTVOPCONEPVL"; //""4JKZIIGFNLOG6MKWHLSGPEBRH5A3JGDTKMV4B3HDYEQWF5ZP5SMB6NHAKFIPUZ7D7HLKLYSW4AJL3VV5E6GUKISFBVYPNDBB6673IFTSXOGG6LIXIOMSQUEA6FHFYNWW";

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
