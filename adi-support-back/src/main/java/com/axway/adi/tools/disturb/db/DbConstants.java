package com.axway.adi.tools.disturb.db;

import java.util.*;

public class DbConstants {
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
