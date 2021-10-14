package com.axway.adi.tools.disturb.db;

import java.util.*;

public class DbConstants {
    public enum Status {
        New, InProgress, Done
    }

    public enum Level {
        Info, Warning, Error
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
