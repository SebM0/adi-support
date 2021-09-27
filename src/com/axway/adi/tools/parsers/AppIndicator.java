package com.axway.adi.tools.parsers;

import java.util.*;
import java.util.stream.*;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

public class AppIndicator extends AppIdentifiable {
    public List<AppEntity> entities = new ArrayList<>();
    public String type = "";
    public String configuration = "";
    public String rhythm;
    public String periodRhythm;
    public String ttl = "";

    public AppIndicator(UUID uuid) {
        super(uuid);
    }

    public boolean isConfigured() {
        return !configuration.isEmpty();
    }

    @Override
    public String toString() {
        String entityNames = entities.stream()
                .map(e -> e.name)
                .sorted(CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(" x "));
        return "[" + entityNames + "]." + name;
    }
}
