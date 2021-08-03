package com.axway.adi.tools.util.db;

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
        FileList
    }

    private DbConstants() {

    }
}
