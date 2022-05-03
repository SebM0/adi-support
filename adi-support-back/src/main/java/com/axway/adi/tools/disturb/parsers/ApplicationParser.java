package com.axway.adi.tools.disturb.parsers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.axway.adi.tools.disturb.db.DbConstants;
import com.axway.adi.tools.disturb.db.DiagnosticResult;
import com.axway.adi.tools.disturb.db.DiagnosticSpecification;
import com.axway.adi.tools.disturb.db.SupportCaseResource;
import com.axway.adi.tools.disturb.parsers.contexts.DiagnosticParseContext;
import com.axway.adi.tools.disturb.parsers.structures.AppEntity;
import com.axway.adi.tools.disturb.parsers.structures.AppFieldType;
import com.axway.adi.tools.disturb.parsers.structures.AppIdentifiable;
import com.axway.adi.tools.disturb.parsers.structures.AppIndicator;
import com.axway.adi.tools.util.FileUtils;

import static com.axway.adi.tools.disturb.DiagnosticCatalog.CAT;
import static java.util.stream.Collectors.*;

public class ApplicationParser extends Parser {
    private static final String XML_ENTITIES = "entities";
    private static final String XML_ENTITY = "entity";
    private static final String XML_INSTANCE = "instance";
    private static final String XML_ATTRIBUTES = "attributes";
    private static final String XML_ATTRIBUTE = "attribute";
    private static final String XML_ATTRIBUTE_VALUE = "attributeValue";
    private static final String XML_RELATIONS = "relations";
    private static final String XML_RELATION = "relation";
    private static final String XML_NAME = "name";
    private static final String XML_UUID = "uuid";
    private static final String XML_VALUE = "value";
    private static final String XML_BLOB_REF = "blobRef";
    private static final String XML_INSTANCE_UUID = "instanceUUID";
    private static final String XML_RELATION_BINDING = "relationBinding";
    private static final String XML_BINDING = "binding";

    private final Map<UUID, AppEntity> entities = new HashMap<>();
    private final Map<UUID, AppIndicator> indicators = new HashMap<>();
    private final Map<UUID, AppIndicatorData> indicatorData = new HashMap<>();
    private final Map<UUID, AppMember> members = new HashMap<>();
    private final Map<UUID, String> rhythms = new HashMap<>();

    public ApplicationParser(SupportCaseResource resource) {
        super(resource);
    }

    protected Stream<Path> filterFiles(Stream<Path> stream) {
        return stream.filter(f -> f.getFileName().toString().equalsIgnoreCase("application.xml")).limit(1);
    }

    @Override
    protected void parseFile(Path filePath, Consumer<DiagnosticResult> resultConsumer) throws IOException {
        // Clears data
        entities.clear();
        indicators.clear();
        indicatorData.clear();
        members.clear();
        rhythms.clear();
        // Load file as XML
        FileInputStream fis = new FileInputStream(filePath.toFile());
        Document document = FileUtils.parseDocument(fis);
        Element xmlApplication = document.getDocumentElement();
        // Parse appx
        parseEntities(xmlApplication);
        parseAttributeValues(xmlApplication);
        parseRelationBindings(xmlApplication);
        // Update global context
        GlobalContext globalContext = resource.getGlobalContext();
        if (globalContext != null)
            globalContext.registerApplication(entities, indicators);
        // run diagnostics
        CAT.getDiagnosticsByType(DbConstants.ResourceType.Appx).forEach(diag -> {
            DiagnosticParseContext<AppIdentifiable> context = createDiagnosticContext(diag);
            entities.values().forEach(e -> context.analyse(getRelativePath(), e));
            indicators.values().forEach(e -> context.analyse(getRelativePath(), e));
            DiagnosticResult result = context.getResult();
            if (result != null) {
                resultConsumer.accept(result);
            }
        });
    }

    private void parseEntities(Element xmlApplication) {
        Element xmlEntities = getChildrenWithName(xmlApplication, XML_ENTITIES);
        if (xmlEntities != null) {
            for (Element xmlEntity : getChildrenElements(xmlEntities, XML_ENTITY)) {
                String entityName = xmlEntity.getAttribute(XML_NAME);
                if (entityName == null) {
                    continue;
                }
                if ("System:Entity".equals(entityName)) {
                    for (Element xmlInstance : getChildrenElements(xmlEntity, XML_INSTANCE)) {
                        AppEntity entity = new AppEntity(getUUID(xmlInstance));
                        entities.put(entity.uuid, entity);
                    }
                    break;
                }
            }
        }
    }

    private void parseAttributeValues(Element xmlApplication) {
        Element xmlAttributes = getChildrenWithName(xmlApplication, XML_ATTRIBUTES);
        if (xmlAttributes != null) {
            for (Element xmlAttribute : getChildrenElements(xmlAttributes, XML_ATTRIBUTE)) {
                UUID attributeUUID = getUUID(xmlAttribute);
                if (attributeUUID == null) {
                    continue;
                }
                String attributeName = xmlAttribute.getAttribute(XML_NAME);
                if (attributeName == null) {
                    continue;
                }
                AppFieldType fieldType = AppFieldType.fromField(attributeName);
                if (fieldType == null) {
                    continue;
                }
                for (Element xmlAttributeValue : getChildrenElements(xmlAttribute, XML_ATTRIBUTE_VALUE)) {
                    String value = "";
                    for (Element xmlValue : getChildrenElements(xmlAttributeValue, XML_VALUE)) {
                        value = xmlValue.getTextContent();
                    }
                    String blobReference = "";
                    for (Element xmlBlobReference : getChildrenElements(xmlAttributeValue, XML_BLOB_REF)) {
                        blobReference = xmlBlobReference.getTextContent();
                    }
                    List<UUID> instances = new ArrayList<>();
                    for (Element xmlInstance : getChildrenElements(xmlAttributeValue, XML_INSTANCE)) {
                        instances.add(getUUID(xmlInstance, XML_INSTANCE_UUID));
                    }
                    if (fieldType == AppFieldType.RhythmName) {
                        rhythms.put(instances.get(0), value);
                    } else if (fieldType.isEntity()) {
                        AppEntity entity = entities.get(instances.get(0));
                        if (entity == null) {
                            continue;
                        }
                        switch (fieldType) {
                            case EntityName:
                                entity.name = value;
                                break;
                            case EntityTTL:
                                entity.ttl = value;
                                break;
                        }
                    } else if (fieldType.isIndicator()) {
                        AppIndicator indicator = indicators.computeIfAbsent(instances.get(0), AppIndicator::new);
                        switch (fieldType) {
                            case IndicatorName:
                                indicator.name = value;
                                break;
                            case IndicatorType:
                                indicator.type = value;
                                break;
                            case IndicatorConfiguration:
                                indicator.configuration = blobReference.isEmpty() ? value : blobReference;
                                break;
                        }
                    } else if (fieldType.isData()) {
                        AppIndicatorData indicatorData = this.indicatorData.computeIfAbsent(instances.get(0), AppIndicatorData::new);
                        switch (fieldType) {
                            case DataMember:
                                indicatorData.dataUUID = UUID.fromString(value);
                                break;
                            case DataInterest:
                                indicatorData.interest = value;
                                break;
                        }
                    } else if (fieldType.isMember()) {
                        AppMember member = this.members.computeIfAbsent(instances.get(0), AppMember::new);
                        if (fieldType == AppFieldType.MemberTTL) {
                            member.ttl = value;
                        }
                    }
                }
            }
        }
    }

    private void parseRelationBindings(Element xmlApplication) {
        Element xmlRelations = getChildrenWithName(xmlApplication, XML_RELATIONS);
        if (xmlRelations == null) {
            return;
        }
        Map<UUID, UUID> roleToEntity = new HashMap<>();
        Map<UUID, List<UUID>> indicatorToRole = new HashMap<>();
        for (Element xmlRelation : getChildrenElements(xmlRelations, XML_RELATION)) {
            UUID relationUUID = getUUID(xmlRelation);
            if (relationUUID == null) {
                continue;
            }
            String relationName = xmlRelation.getAttribute(XML_NAME);
            if (relationName == null) {
                continue;
            }
            AppFieldType fieldType = AppFieldType.fromField(relationName);
            if (fieldType == null) {
                continue;
            }
            for (Element xmlRelationBinding : getChildrenElements(xmlRelation, XML_RELATION_BINDING)) {
                UUID instance = null;
                for (Element xmlInstance : getChildrenElements(xmlRelationBinding, XML_INSTANCE)) {
                    instance = getUUID(xmlInstance, XML_INSTANCE_UUID);
                }
                List<UUID> bindings = new ArrayList<>();
                for (Element xmlValue : getChildrenElements(xmlRelationBinding, XML_BINDING)) {
                    for (Element xmlBinding : getChildrenElements(xmlValue, XML_INSTANCE)) {
                        bindings.add(getUUID(xmlBinding, XML_INSTANCE_UUID));
                    }
                }
                switch (fieldType) {
                    case Indicator2Role:
                        indicatorToRole.put(instance, bindings);
                        break;
                    case Role2Entity:
                        roleToEntity.put(instance, bindings.get(0));
                        break;
                    case Indicator2Period:
                        indicators.get(instance).periodRhythm = rhythms.get(bindings.get(0));
                        break;
                    case Member2Rhythm:
                        AppMember member = this.members.computeIfAbsent(instance, AppMember::new);
                        member.rhythm = rhythms.get(bindings.get(0));
                        break;
                    case Data2Indicator:
                        indicatorData.get(instance).indicatorUUID = bindings.get(0);
                }
            }
        }
        // bind entities
        indicators.values().forEach(attribute -> attribute.entities.addAll(indicatorToRole.get(attribute.uuid).stream() //
                                          .map(roleToEntity::get).filter(Objects::nonNull) //
                                          .map(entities::get).filter(Objects::nonNull) //
                                          .collect(toList())));
        // bind indicator data & members
        indicatorData.values().forEach(data -> {
            if ("Value".equals(data.interest)) {
                AppIndicator indicator = indicators.get(data.indicatorUUID);
                AppMember member = members.get(data.dataUUID);
                if (indicator != null && member != null) {
                    indicator.rhythm = member.rhythm;
                    indicator.ttl = member.ttl;
                    indicator.memberUuid = member.uuid;
                }
            } else {
                // remove virtual indicators
                indicators.remove(data.indicatorUUID);
            }
        });
        indicatorData.clear();
        members.clear();
    }

    private static Element getChildrenWithName(Element node, String name) {
        List<Element> child = getChildrenElements(node, name);
        return child.size() == 1 ? child.get(0) : null;
    }

    private static UUID getUUID(Element node) {
        return getUUID(node, XML_UUID);
    }

    private static UUID getUUID(Element node, String attr) {
        String uuidAttribute = node.getAttribute(attr);
        if (uuidAttribute == null) {
            return null;
        }
        return UUID.fromString(uuidAttribute);
    }

    public static List<Element> getChildrenElements(Node node, String name) {
        NodeList childNodes = node.getChildNodes();
        return IntStream.
                range(0, childNodes.getLength()).
                mapToObj(childNodes::item).
                filter(c -> c.getNodeType() == Node.ELEMENT_NODE).
                filter(Element.class::isInstance).
                map(Element.class::cast).
                filter(n -> name.equals(n.getTagName())).
                collect(toList());
    }

    @SuppressWarnings("unchecked")
    private DiagnosticParseContext<AppIdentifiable> createDiagnosticContext(DiagnosticSpecification diag) {
        return (DiagnosticParseContext<AppIdentifiable>) diag.createContext(resource);
    }

    @Override
    public int getSize() {
        return entities.size() + indicators.size();
    }

    private static class AppIndicatorData extends AppIdentifiable {
        public UUID indicatorUUID = null;
        public UUID dataUUID = null;
        public String interest = "";

        public AppIndicatorData(UUID uuid) {
            super(uuid);
        }
    }

    private static class AppMember extends AppIdentifiable {
        public String rhythm = null;
        public String ttl = "";

        public AppMember(UUID uuid) {
            super(uuid);
        }
    }
}
