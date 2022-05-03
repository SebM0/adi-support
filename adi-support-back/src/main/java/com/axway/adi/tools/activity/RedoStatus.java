package com.axway.adi.tools.activity;

import java.util.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static java.util.stream.Collectors.*;

public class RedoStatus {
    private final String name;
    private final long checkpoint;
    private final long dbOffset;
    private final long wfOffset;

    public RedoStatus(String name, long checkpoint, long dbOffset, long wfOffset) {
        this.name = name;
        this.checkpoint = checkpoint;
        this.dbOffset = dbOffset;
        this.wfOffset = wfOffset;
    }

    public String getName() {
        return name;
    }

    public long getCheckpoint() {
        return checkpoint;
    }

    public long getDbOffset() {
        return dbOffset;
    }

    public long getWfOffset() {
        return wfOffset;
    }

    @Override
    public String toString() {
        if (isReplica()) {
            return String.format("%s: checkpoint: %d, DB offset: %d", name, checkpoint, dbOffset);
        }
        return String.format("%s: checkpoint: %d, DB offset: %d, WF offset: %d", name, checkpoint, dbOffset, wfOffset);
    }

    private boolean isReplica() {
        return name.startsWith("replica:");
    }

    public static RedoStatus readFromJson(String name, JsonObject args) {
        JsonElement redoLogCheckpoint = args.get("redoLogCheckpoint");
        if (redoLogCheckpoint == null) {
            return null;
        }
        return new RedoStatus(name, redoLogCheckpoint.getAsLong(), //
                              args.get("dbRedoLogOffset").getAsLong(), //
                              args.get("wfRedoLogOffset").getAsLong());
    }

    public static List<RedoStatus> searchConsumers(JsonObject args) {
        return args.entrySet().stream().filter(e -> e.getKey().startsWith("replica:") || e.getKey().startsWith("backup:")) //
                .map(e -> RedoStatus.readFromJson(e.getKey(), e.getValue().getAsJsonObject())) //
                .collect(toList());
    }
}
