package com.axway.adi.tools.disturb.parsers.structures;

import java.util.*;

public class AppEntity extends AppIdentifiable {
    public boolean partitioned;
    public String ttl = "";

    public AppEntity(UUID uuid) {
        super(uuid);
    }
}
