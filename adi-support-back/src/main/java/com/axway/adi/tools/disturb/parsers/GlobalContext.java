package com.axway.adi.tools.disturb.parsers;

import java.util.*;
import com.axway.adi.tools.disturb.parsers.structures.AppEntity;
import com.axway.adi.tools.disturb.parsers.structures.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.structures.AppIndicator;

public class GlobalContext {
    private final Map<UUID, AppEntity> entities = new HashMap<>();
    private final Map<UUID, AppIndicator> indicators = new HashMap<>();
    private final Map<UUID, String> internals = Map.of( //
                                                        UUID.fromString("0002C4ED-0000-0000-1506-64eb32d06800"), "Entity input ScanRange", //
                                                        UUID.fromString("0002C4ED-0000-0000-1506-64eb32d07000"), "Member input ScanRange", //
                                                        UUID.fromString("00000008-0000-0000-1387-a8bcc0601800"), "Most current TT", //
                                                        UUID.fromString("0000001e-0000-0000-1427-f983b4100000"), "Last login date", //
                                                        UUID.fromString("0000001e-0000-0000-1427-f983b4100800"), "Last logout date" //
    );

    public void registerApplication(Map<UUID, AppEntity> entities, Map<UUID, AppIndicator> indicators) {
        this.entities.putAll(entities);
        this.indicators.putAll(indicators);
    }

    public AppIdentifiable getItem(String storeName) {
        //Column name starts: "U_" for UUID column, "I_" for instance, "A_" for attribute, "R_" for relations
        char prefix = storeName.charAt(0);
        UUID uuid = UUID.fromString(storeName.substring(2));
        if (prefix == 'A' || prefix == 'R') {
            return indicators.values().stream().filter(ind -> uuid.equals(ind.memberUuid)).findFirst().orElse(null);
        }
        if (prefix == 'I') {
            return entities.get(uuid);
        }
        return null;
    }

    public String getItemName(String storeName) {
        AppIdentifiable item = getItem(storeName);
        if (item != null) {
            // Customer item
            return item.toString();
        }
        // internal items ?
        UUID uuid = UUID.fromString(storeName.substring(2));
        String internalStuff = internals.get(uuid);
        if (internalStuff != null) {
            return internalStuff + " (internal)";
        }
        // unknown
        return storeName;
    }
}
