package com.axway.adi.tools.disturb.parsers.structures;

public class FileDescription {
    public String type;
    public String path;
    public long size;
    public String lastModified;

    public boolean isFile() {
        return "F".equals(type);
    }

    public boolean isDirectory() {
        return "D".equals(type);
    }

    public static FileDescription parse(String[] split) {
        if (split.length != 4)
            return null;
        FileDescription fd = new FileDescription();
        fd.type = split[0];
        fd.path = split[1];
        fd.size = Long.parseLong(split[2]);
        fd.lastModified = split[3];
        return fd;
    }
}
