package com.axway.adi.tools.xparser;

import java.util.*;

public enum Mashlet {
    Baseline("com.systar.sulfur.mashlets.model.format.mashlet.CompositeHistoricalBaselineMashletFormatPO"), //
    MultiHistorical("com.systar.sulfur.mashlets.model.format.mashlet.MultiHistoricalMashletFormatPO"), //
    Gauge("com.systar.sulfur.mashlets.model.format.mashlet.GaugeMashletFormatPO"), //
    Pie("com.systar.sulfur.mashlets.model.format.mashlet.PieMashletFormatPO"), //
    Spark("com.systar.sulfur.mashlets.model.format.mashlet.SparkColumnMashletFormatPO"), //
    Threshold("com.systar.sulfur.mashlets.model.format.mashlet.CompositeThresholdLevelMashletFormatPO"), //
    Other(""), //
    ;

    private final String m_type;

    Mashlet(String type) {
        m_type = type;
    }

    public boolean isChart() {
        return EnumSet.of(Baseline, MultiHistorical, Pie, Gauge).contains(this);
    }

    public static Mashlet fromType(String type) {
        if (type == null) {
            return null;
        }
        for (Mashlet value : Mashlet.values()) {
            if (value.m_type.compareToIgnoreCase(type) == 0) {
                return value;
            }
        }
        return Other;
    }
}
