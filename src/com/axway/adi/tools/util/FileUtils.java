package com.axway.adi.tools.util;

import java.text.DecimalFormat;

public class FileUtils {
    public static final int MBYTES = 1024 * 1024;
    public static final int KBYTES = 1024;
    public static final int GBYTES = 1024 * 1024 * 1024;

    public static String formatFileSize(long value) {
        if (value < KBYTES) {
            return value + " Bytes";
        } else if (value < MBYTES) {
            return formatSize(value, KBYTES, "KB");
        } else if (value < GBYTES) {
            return formatSize(value, MBYTES, "MB");
        } else {
            return formatSize(value, GBYTES, "GB");
        }
    }

    private static String formatSize(long fileSize, int unit, String unitString) {
        double value = Math.round((fileSize * 10) / (unit * 1.0d)) / 10.0d;
        DecimalFormat format = (value < 10) ? new DecimalFormat("#0.0") : new DecimalFormat("#0");
        return format.format(value) + " " + unitString;
    }
}
