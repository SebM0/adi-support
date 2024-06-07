package com.axway.adi.tools.util;

public class StringHelper {

    public static String normalize(String value) {
        String normalized = value.replaceAll("\n", "");
        normalized = normalized.replaceAll("'", "");
        return normalized;
    }

    private StringHelper() {
    }
}
