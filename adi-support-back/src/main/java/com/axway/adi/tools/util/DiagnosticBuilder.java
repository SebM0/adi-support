package com.axway.adi.tools.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DiagnosticBuilder {
    private static final int VERSION = 1;
    private static final String RULES = "rules";
    private static final String RULE = "rule";
    private static final String KIND = "kind";
    private static final String KIND_STACK = "stack";
    private final JsonObject diagnostic = new JsonObject();

    public DiagnosticBuilder() {
        diagnostic.addProperty("version", VERSION);
    }

    public DiagnosticBuilder addThreadDumpStackRule(String regExp) {
        JsonElement rules = diagnostic.get(RULES);
        if (rules == null) {
            rules = new JsonArray();
            diagnostic.add(RULES, rules);
        }
        JsonObject rule = new JsonObject();
        rule.addProperty(KIND, KIND_STACK);
        rule.addProperty(RULE, regExp);
        rules.getAsJsonArray().add(rule);
        return this;
    }

    public String build() {
        return diagnostic.toString();
    }
}
