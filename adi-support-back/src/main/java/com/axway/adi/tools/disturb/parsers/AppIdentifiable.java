package com.axway.adi.tools.disturb.parsers;

import java.util.*;

public class AppIdentifiable {
    public UUID uuid = null;
    public String name = "";

    public AppIdentifiable(UUID uuid) {
        this.uuid = uuid;
    }
}