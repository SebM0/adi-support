package com.axway.adi.tools.disturb.parsers;

public enum AppFieldType {
    // attributes
    RhythmName("[System:Rhythm].System:Name"), //
    EntityName("[System:Entity].System:Name"), //
    EntityTTL("[System:Entity].System:TimeToLiveRequested"), //
    IndicatorName("[Semantic:Indicator].Semantic:name"), //
    IndicatorType("[Semantic:Indicator].Semantic:type"), //
    IndicatorConfiguration("[Semantic:Indicator].Semantic:configurationBlob"), //
    DataMember("[Semantic:IndicatorData].Semantic:dataUUID"), //
    DataInterest("[Semantic:IndicatorData].Semantic:interest"), //
    MemberTTL("[System:Member].System:TimeToLiveRequested"), //
    // relations
    Indicator2Role("[Semantic:Indicator].Semantic:roles"), //
    Role2Entity("[System:EntityRole].System:Entity"), //
    Indicator2Period("[Semantic:Indicator].Semantic:periodRhythm"), //
    Member2Rhythm("[System:Member].System:ValidTimeRhythm"), //
    Data2Indicator("[Semantic:IndicatorData].Semantic:indicator"), //
    ;
    private final String field;
    AppFieldType(String field) {
        this.field = field;
    }

    public static AppFieldType fromField(String field) {
        for (AppFieldType type : AppFieldType.values()) {
            if (type.field.equals(field)) {
                return type;
            }
        }
        return null;
    }

    public boolean isIndicator() {
        return this.name().startsWith("Indicator");
    }

    public boolean isEntity() {
        return this.name().startsWith("Entity");
    }

    public boolean isData() {
        return this.name().startsWith("Data");
    }

    public boolean isMember() {
        return this.name().startsWith("Member");
    }
}
