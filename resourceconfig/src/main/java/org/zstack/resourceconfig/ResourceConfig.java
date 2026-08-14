package org.zstack.resourceconfig;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.*;
import org.zstack.core.db.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;
import static org.zstack.resourceconfig.ResourceConfigCanonicalEvents.*;

/**
 * Created by MaJin on 2019/2/23.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceConfig {
    private static final CLogger logger = Utils.getLogger(ResourceConfig.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private EventFacade evtf;

    protected GlobalConfig globalConfig;
    private List<Class> resourceClasses;
    private Map<String, ResourceConfigGetter> configGetter = new HashMap<>();
    private List<ResourceConfigUpdateExtensionPoint> localUpdateExtensions = new ArrayList<>();
    private List<ResourceConfigUpdateExtensionPoint> updateExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteExtensionPoint> localDeleteExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteExtensionPoint> deleteExtensions = new ArrayList<>();
    private List<ResourceConfigValidatorExtensionPoint> validatorExtensions = new ArrayList<>();

    public static ResourceConfig valueOf(GlobalConfig globalConfig, BindResourceConfig bindInfo) {
        ResourceConfig result = new ResourceConfig();
        result.globalConfig = globalConfig;
        result.resourceClasses = Arrays.asList(bindInfo.value());
        return result;
    }

    public void installLocalUpdateExtension(ResourceConfigUpdateExtensionPoint ext) {
        localUpdateExtensions.add(ext);
    }

    public void installUpdateExtension(ResourceConfigUpdateExtensionPoint ext) {
        updateExtensions.add(ext);
    }

    public void installLocalDeleteExtension(ResourceConfigDeleteExtensionPoint ext) {
        localDeleteExtensions.add(ext);
    }

    public void installDeleteExtension(ResourceConfigDeleteExtensionPoint ext) {
        deleteExtensions.add(ext);
    }

    public void installValidatorExtension(ResourceConfigValidatorExtensionPoint ext) {
        validatorExtensions.add(ext);
    }

    public void validateOnly(String newValue) {
        String oldValue = globalConfig.value();
        globalConfig.getValidators().forEach(it ->
                it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
    }

    public void validateNewValue(String resourceUuid, String newValue) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        globalConfig.getValidators().forEach(it ->
                it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
        validatorExtensions.forEach(it -> it.validateResourceConfig(resourceUuid, oldValue, newValue));
    }

    public void updateValue(String resourceUuid, String newValue) {
        String resourceType = getResourceType(resourceUuid);
        updateValue(resourceUuid, resourceType, newValue, true);
    }

    public void deleteValue(String resourceUuid) {
        String resourceType = getResourceType(resourceUuid);
        deleteValue(resourceUuid, resourceType, true);
    }

    public <T> T defaultValue(Class<T> clz) {
        return globalConfig.value(clz);
    }

    public <T> T getResourceConfigValue(String resourceUuid, Class<T> clz) {
        String value = getResourceConfigValue(resourceUuid);
        return TypeUtils.stringToValue(value, clz);
    }

    public <T> T getResourceConfigValueByResourceType(String resourceUuid, String resourceType, Class<T> clz) {
        String value = getResourceConfigValueByResourceType(resourceUuid, resourceType);
        return TypeUtils.stringToValue(value, clz);
    }

    public <T> Map<String, T> getResourceConfigValues(List<String> resourceUuids, Class<T> clz) {
        Map<String, T> values = new HashMap<>();
        getResourceConfigValues(resourceUuids).forEach((key, value) -> values.put(key, TypeUtils.stringToValue(value, clz)));
        return values;
    }

    void init() {
        installEventTrigger();
        initResourceConfigNodes();
    }

    private void installEventTrigger() {
        evtf.on(s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                UpdateEvent evt = (UpdateEvent) data;
                String newValue = Q.New(ResourceConfigVO.class).select(ResourceConfigVO_.value)
                        .eq(ResourceConfigVO_.resourceUuid, evt.getResourceUuid())
                        .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                        .eq(ResourceConfigVO_.name, globalConfig.getName())
                        .findValue();

                updateValue(evt.getResourceUuid(), evt.getResourceType(), newValue, false);
                logger.info(String.format("ResourceConfig [resourceUuid:%s, category:%s, name:%s] was updated in other" +
                                " management node[uuid:%s], in line with that change, updated ours. %s --> %s",
                        evt.getResourceUuid(), globalConfig.getCategory(), globalConfig.getName(), nodeUuid, evt.getOldValue(), newValue));
            }
        });

        evtf.on(s(ResourceConfigCanonicalEvents.DELETE_EVENT_PATH).formatByMap(map(
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                DeleteEvent evt = (DeleteEvent) data;
                deleteValue(evt.getResourceUuid(), evt.getResourceType(), false);
                logger.info(String.format("ResourceConfig[resourceUuid: %s category: %s, name: %s] was deleted from" +
                                " other management node[uuid:%s], in line with that change, deleted ours.",
                        evt.getResourceUuid(), globalConfig.getCategory(), globalConfig.getName(), nodeUuid));
            }
        });
    }

    private void initResourceConfigNodes() {
        for (int i = 0; i < resourceClasses.size(); i++) {
            Class clz = resourceClasses.get(i);
            ResourceConfigGetter getter = new ResourceConfigGetter();
            getter.init(resourceClasses.subList(i, resourceClasses.size()));
            configGetter.put(clz.getSimpleName(), getter);
        }
    }

    private void updateValue(String resourceUuid, String resourceType, String newValue, boolean localUpdate) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        if (localUpdate) {
            globalConfig.getValidators().forEach(it ->
                    it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
            validatorExtensions.forEach(it -> it.validateResourceConfig(resourceUuid, oldValue, newValue));
            updateValueInDb(resourceUuid, resourceType, newValue);
            localUpdateExtensions.forEach(it -> it.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue));
        }

        updateExtensions.forEach(it -> it.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue));

        if (localUpdate) {
            UpdateEvent evt = new UpdateEvent();
            evt.setResourceUuid(resourceUuid);
            evt.setOldValue(oldValue);
            evtf.fire(makeUpdateEventPath(), evt);
        }

        logger.debug(String.format("updated resource config[resourceUuid:%s, resourceType:%s, category:%s, name:%s]: %s to %s",
                resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
    }

    private void deleteValue(String resourceUuid, String resourceType, boolean localDelete) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        if (localDelete) {
            deleteInDb(resourceUuid);
            localDeleteExtensions.forEach(it -> it.deleteResourceConfig(this, resourceUuid, resourceType, originValue));
        }

        deleteExtensions.forEach(it -> it.deleteResourceConfig(this, resourceUuid, resourceType, originValue));

        if (localDelete) {
            DeleteEvent evt = new DeleteEvent();
            evt.setResourceUuid(resourceUuid);
            evt.setResourceType(resourceType);
            evt.setOldValue(oldValue);
            evtf.fire(makeDeleteEventPath(), evt);
        }

        logger.debug(String.format("deleted resource config[resourceUuid:%s, resourceType:%s, category:%s, name:%s]",
                resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
    }

    @Transactional(readOnly = true)
    protected String getResourceConfigValue(String resourceUuid) {
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (resourceType == null) {
            logger.warn(String.format("no resource[uuid:%s] found, cannot get it's resource config," +
                    " use global config instead", resourceUuid));
            return globalConfig.value();
        }

        ResourceConfigGetter getter = configGetter.get(resourceType);
        if (getter == null) {
            logger.warn(String.format("resource[uuid:%s, type:%s] is not bound to global config[category:%s, name:%s]," +
                    " use global config instead", resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
            return globalConfig.value();
        }

        return getter.getResourceConfigValue(resourceUuid);
    }

    @Transactional(readOnly = true)
    protected String getResourceConfigValueByResourceType(String resourceUuid, String type) {
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (resourceType == null || !resourceType.equals(type)) {
            logger.warn(String.format("no resource[uuid:%s] by resourceType[%s] found, cannot get it's resource config", resourceUuid, type));
            return null;
        }

        ResourceConfigGetter getter = configGetter.get(resourceType);
        if (getter == null) {
            logger.warn(String.format("resource[uuid:%s, type:%s] is not bound to global config[category:%s, name:%s]",
                    resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
            return null;
        }

        return loadConfigValue(resourceUuid);
    }

    @Transactional(readOnly = true)
    protected Map<String, String> getResourceConfigValues(List<String> resourceUuids) {
        Map<String, String> valuesByResourceUuids = new HashMap<>();

        if (resourceUuids.isEmpty()) {
            return valuesByResourceUuids;
        }

        List<Tuple> resourceTypeUuidPairs = Q.New(ResourceVO.class).select(ResourceVO_.resourceType, ResourceVO_.uuid)
                .in(ResourceVO_.uuid, resourceUuids).listTuple();

        if (resourceTypeUuidPairs.isEmpty()) {
            logger.warn("no resource found, cannot get it's resource config, use global config instead");

            resourceUuids.forEach(it -> valuesByResourceUuids.put(it, globalConfig.value()));
            return valuesByResourceUuids;
        }

        resourceUuids.removeAll(resourceTypeUuidPairs.stream().map(it -> it.get(1, String.class)).collect(Collectors.toSet()));
        resourceUuids.forEach(it -> valuesByResourceUuids.put(it, globalConfig.value()));

        Map<String, List<String>> typeByResourceUuids = groupResourceUuidsByType(resourceTypeUuidPairs);

        if (typeByResourceUuids.keySet().size() >= 2) {
            throw new OperationFailureException(
                    operr("resources has inconsistent resourceTypes. Details: %s", typeByResourceUuids.toString()));
        }

        typeByResourceUuids.forEach((resourceType, resUuids) -> {
            ResourceConfigGetter getter = configGetter.get(resourceType);
            if (getter == null) {
                logger.warn(String.format("resource[type:%s] is not bound to global config[category:%s, name:%s]," +
                        " use global config instead", resourceType, globalConfig.getCategory(), globalConfig.getName()));

                resUuids.forEach(uuid -> valuesByResourceUuids.put(uuid, globalConfig.value()));
                return;
            }
            valuesByResourceUuids.putAll(getter.getResourceConfigValues(resUuids));
        });

        return valuesByResourceUuids;
    }

    private Map<String, List<String>> groupResourceUuidsByType(List<Tuple> resourceTypeUuidPairs) {
        return resourceTypeUuidPairs.stream().collect(Collectors.groupingBy(pair -> pair.get(0, String.class),
                Collectors.mapping(pair -> pair.get(1, String.class), Collectors.toList())));
    }

    public List<ResourceConfigInventory> getEffectiveResourceConfigs(String resourceUuid) {
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (resourceType == null) {
            logger.warn(String.format("no resource[uuid:%s] found, cannot get it's resource config," +
                    " use global config instead", resourceUuid));
            return Collections.emptyList();
        }

        ResourceConfigGetter getter = configGetter.get(resourceType);
        if (getter == null) {
            logger.warn(String.format("resource[uuid:%s, type:%s] is not bound to global config[category:%s, name:%s]," +
                    " use global config instead", resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
            return Collections.emptyList();
        }

        return getter.getConnectedResourceConfigs(resourceUuid);
    }

    private class ResourceConfigGetter {
        String resourceType;
        List<String> parentTypeSql = new ArrayList<>();
        List<String> parentResourceUuidPairsSql = new ArrayList<>();

        private String getResourceConfigValue(String resourceUuid) {
            String v = loadConfigValue(resourceUuid);
            if (v != null) {
                return v;
            }

            for (String sql : parentTypeSql) {
                String resUuid = SQL.New(String.format(sql, resourceUuid), String.class).find();
                if (resUuid == null) {
                    continue;
                }

                v = loadConfigValue(resUuid);
                if (v != null) {
                    return v;
                }
            }

            return globalConfig.value();
        }

        private Map<String, String> getResourceConfigValues(List<String> resourceUuids) {
            Map<String, String> valuesByResourceUuids = new HashMap<>();

            loadResourceUuidsAndValues(resourceUuids).forEach(it -> valuesByResourceUuids.put(it.get(0, String.class), it.get(1, String.class)));
            resourceUuids.removeAll(valuesByResourceUuids.keySet());

            for (String sql : parentResourceUuidPairsSql) {
                List<Tuple> parentResourceUuidPairs =
                        SQL.New(String.format(sql, "'" + String.join("','", resourceUuids) + "'"), Tuple.class).list();
                if (parentResourceUuidPairs.isEmpty()) {
                    continue;
                }

                Map<String, List<String>> parentUuidAndResourceUuids = parentResourceUuidPairs.stream()
                        .collect(Collectors.groupingBy(tuple -> tuple.get(0, String.class),
                                Collectors.mapping(tuple -> tuple.get(1, String.class), Collectors.toList())));

                loadResourceUuidsAndValues(new ArrayList<>(parentUuidAndResourceUuids.keySet())).forEach(tuple -> {
                    if (parentUuidAndResourceUuids.containsKey(tuple.get(0, String.class))) {
                        List<String> uuids = parentUuidAndResourceUuids.get(tuple.get(0, String.class));
                        uuids.forEach(resourceUuid -> valuesByResourceUuids.put(resourceUuid, tuple.get(1, String.class)));
                        resourceUuids.removeAll(uuids);
                    }
                });
            }

            resourceUuids.forEach(it -> valuesByResourceUuids.put(it, globalConfig.value()));
            return valuesByResourceUuids;
        }

        private List<ResourceConfigInventory> getConnectedResourceConfigs(String resourceUuid) {
            List<ResourceConfigInventory> results = new ArrayList<>();
            Optional.ofNullable(loadConfig(resourceUuid)).ifPresent(it ->
                    results.add(ResourceConfigInventory.valueOf(it)));
            for (String sql : parentTypeSql) {
                String resUuid = SQL.New(String.format(sql, resourceUuid), String.class).find();
                if (resUuid == null) {
                    continue;
                }

                Optional.ofNullable(loadConfig(resUuid)).ifPresent(it ->
                        results.add(ResourceConfigInventory.valueOf(it)));
            }
            return results;
        }

        private void init(List<Class> connectedClasses) {
            Class resourceClass = connectedClasses.get(0);
            for (Class parentClass : connectedClasses.subList(1, connectedClasses.size())) {
                Optional.ofNullable(DBGraph.findVerticesWithSmallestWeight(resourceClass, parentClass)).ifPresent(vertex ->
                        parentTypeSql.add(vertex.toSQL("uuid", SimpleQuery.Op.EQ, "'%s'")));
            }

            for (Class parentClass : connectedClasses.subList(1, connectedClasses.size())) {
                Optional.ofNullable(DBGraph.findVerticesWithSmallestWeight(resourceClass, parentClass)).ifPresent(vertex ->
                        parentResourceUuidPairsSql.add(vertex.toBidirectionalSQL("uuid", SimpleQuery.Op.IN, "(%s)")));
            }

            resourceType = resourceClass.getSimpleName();
        }
    }

    @Transactional
    protected void updateValueInDb(String resourceUuid, String resourceType, String newValue) {
        ResourceConfigVO vo = loadConfig(resourceUuid);
        if (vo != null) {
            vo.setValue(newValue);
            dbf.getEntityManager().merge(vo);
            return;
        }

        vo = new ResourceConfigVO();
        vo.setUuid(Platform.getUuid());
        vo.setCategory(globalConfig.getCategory());
        vo.setName(globalConfig.getName());
        vo.setValue(newValue);
        vo.setDescription(globalConfig.getDescription());
        vo.setResourceUuid(resourceUuid);
        vo.setResourceType(resourceType);
        dbf.getEntityManager().persist(vo);
    }

    protected void deleteInDb(String resourceUuid) {
        SQL.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .delete();
    }

    private String getResourceType(String resourceUuid) {
        String resourceType = Q.New(ResourceVO.class).eq(ResourceVO_.uuid, resourceUuid).select(ResourceVO_.resourceType).findValue();
        if (resourceType == null) {
            throw new OperationFailureException(operr("cannot find resource[uuid: %s]", resourceUuid));
        }

        if (!configGetter.containsKey(resourceType)) {
            throw new OperationFailureException(operr("ResourceConfig [category:%s, name:%s]" +
                    " cannot bind to resourceType: %s", globalConfig.getCategory(), globalConfig.getName(), resourceType));
        }
        return resourceType;
    }

    private String makeUpdateEventPath() {
        return s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        ));
    }

    private String makeDeleteEventPath() {
        return s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        ));
    }

    ResourceConfigVO loadConfig(String resourceUuid) {
        return Q.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .find();
    }

    public boolean resourceConfigCreated(String resourceUuid) {
        return Q.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .isExists();
    }

    public boolean isMatch(String identity) {
        return Objects.equals(globalConfig.getIdentity(), identity);
    }

    public String getResourceConfigUuid(String resourceUuid, String value) {
        return Q.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .eq(ResourceConfigVO_.value, value)
                .select(ResourceConfigVO_.uuid).findValue();
    }

    private String loadConfigValue(String resourceUuid) {
        return Q.New(ResourceConfigVO.class).select(ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .findValue();
    }

    private List<Tuple> loadResourceUuidsAndValues(List<String> resourceUuids) {
        return Q.New(ResourceConfigVO.class)
                .select(ResourceConfigVO_.resourceUuid, ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .in(ResourceConfigVO_.resourceUuid, resourceUuids).listTuple();
    }

    List<Class> getResourceClasses() {
        return resourceClasses;
    }
}
