package com.axway.adi.tools.util;

import java.text.DecimalFormat;

/**
 * This class helps displaying a memory size in a human readable format.
 * <p>
 * We are using the binary units as defined by the Institute of Electrical and Electronics Engineers,
 * you can find more info here https://en.wikipedia.org/wiki/Orders_of_magnitude_(data)
 */
public final class MemorySizeFormatter {
    private static final long KBYTES = 1024L;
    private static final long MBYTES = 1024L * 1024L;
    private static final long GBYTES = 1024L * 1024L * 1024L;
    private static final long TBYTES = 1024L * 1024L * 1024L * 1024L;

    private MemorySizeFormatter() {
        // Prevent instantiation
    }

    /**
     * Provides a human readable representation of a size in bytes.
     *
     * @param bytes Bytes
     * @return human readable representation
     */
    public static String toHumanReadableSize(long bytes) {
        if (bytes < KBYTES) {
            return bytes + " B";
        } else if (bytes < MBYTES) {
            return formatSize(bytes, KBYTES) + " KB";
        } else if (bytes < GBYTES) {
            return formatSize(bytes, MBYTES) + " MB";
        } else if (bytes < TBYTES) {
            return formatSize(bytes, GBYTES) + " GB";
        } else {
            return formatSize(bytes, TBYTES) + " TB";
        }
    }

    public static String toHumanAndRealSize(long bytes) {
        String readableSize = toHumanReadableSize(bytes);
        return bytes >= KBYTES ? readableSize + " (" + bytes + " B)" : readableSize;
    }

    private static String formatSize(long fileSize, long unit) {
        double value = Math.round((fileSize * 10L) / (unit * 1.0d)) / 10.0d;
        DecimalFormat format = (value < 10L) ? new DecimalFormat("#0.0") : new DecimalFormat("#0");
        return format.format(value);
    }
}
