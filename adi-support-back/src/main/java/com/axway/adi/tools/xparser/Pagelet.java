package com.axway.adi.tools.xparser;

public enum Pagelet {
    Instance("com.systar.silicon.querypagelets.model.InstancePageletFormatPO"), //
    ImageMap("com.systar.silicon.querypagelets.model.imagemappagelet.ImageMapFormatPO"), //
    DataGrid("com.systar.silicon.querypagelets.model.DataGridPageletFormatPO"), //
    ;

    private final String m_type;

    Pagelet(String type) {
        m_type = type;
    }

    public static Pagelet fromType(String type) {
        if (type == null) {
            return null;
        }
        for (Pagelet value : Pagelet.values()) {
            if (value.m_type.compareToIgnoreCase(type) == 0) {
                return value;
            }
        }
        return null;
    }
}
