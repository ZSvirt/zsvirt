package org.zstack.resourceconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.*;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class ResourceConfigFacadeImpl extends AbstractService implements ResourceConfigFacade {
    private static final CLogger logger = Utils.getLogger(ResourceConfigFacadeImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private GlobalConfigFacade gcf;

    protected Map<String, ResourceConfig> resourceConfigs = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateResourceConfigMsg) {
            handle((APIUpdateResourceConfigMsg) msg);
        } else if (msg instanceof APIUpdateResourceConfigsMsg) {
            handle((APIUpdateResourceConfigsMsg) msg);
        }else if (msg instanceof APIDeleteResourceConfigMsg) {
            handle((APIDeleteResourceConfigMsg) msg);
        } else if (msg instanceof APIGetResourceBindableConfigMsg) {
            handle((APIGetResourceBindableConfigMsg) msg);
        } else if (msg instanceof APIGetResourceConfigMsg) {
            handle((APIGetResourceConfigMsg) msg);
        } else if (msg instanceof APIGetResourceConfigsMsg) {
            handle((APIGetResourceConfigsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteResourceConfigMsg msg) {
        ResourceConfig rc = getResourceConfig(msg.getIdentity());
        rc.deleteValue(msg.getResourceUuid());
        bus.publish(new APIDeleteResourceConfigEvent(msg.getId()));
    }

    private void handle(APIUpdateResourceConfigMsg msg) {
        ResourceConfig rc = getResourceConfig(msg.getIdentity());
        APIUpdateResourceConfigEvent evt = new APIUpdateResourceConfigEvent(msg.getId());

        try {
            rc.updateValue(msg.getResourceUuid(), msg.getValue());
            evt.setInventory(ResourceConfigInventory.valueOf(rc.loadConfig(msg.getResourceUuid())));
        } catch (GlobalConfigException e) {
            evt.setError(operr(e.getMessage()));
        }

        bus.publish(evt);
    }

    private void handle(APIUpdateResourceConfigsMsg msg) {
        List<String> identities = new ArrayList<>();
        for (APIUpdateResourceConfigsMsg.ResourceConfigAO resourceConfigAO : msg.getResourceConfigs()) {
            String identity = GlobalConfig.produceIdentity(resourceConfigAO.getCategory(), resourceConfigAO.getName());
            identities.add(identity);
            ResourceConfig rc = getResourceConfig(identity);
            try {
                rc.updateValue(msg.getResourceUuid(), resourceConfigAO.getValue());
            } catch (Exception e) {
                logger.debug(String.format("updated resource config[resourceUuid:%s, category:%s, name:%s] to %s failed",
                        msg.getResourceUuid(),  resourceConfigAO.getCategory(), resourceConfigAO.getName(), resourceConfigAO.getValue()));
            } finally {
                continue;
            }
        }

        APIUpdateResourceConfigsEvent evt = new APIUpdateResourceConfigsEvent(msg.getId());
        evt.setInventories(new ArrayList<>());
        for (String identity : identities) {
            ResourceConfig rc = getResourceConfig(identity);
            List<ResourceConfigInventory> configs = rc.getEffectiveResourceConfigs(msg.getResourceUuid());
            ResourceConfigStruct struct = new ResourceConfigStruct();
            struct.setEffectiveConfigs(configs);
            struct.setName(rc.globalConfig.getName());
            struct.setValue(configs.isEmpty() ? rc.defaultValue(String.class) : configs.get(0).getValue());

            evt.getInventories().add(struct);
        }

        bus.publish(evt);
    }

    private void handle(APIGetResourceBindableConfigMsg msg) {
        APIGetResourceBindableConfigReply reply = new APIGetResourceBindableConfigReply();
        List<APIGetResourceBindableConfigReply.ResourceBindableConfigStruct> results = new ArrayList<>();
        if (msg.getCategory() == null) {
            results.addAll(APIGetResourceBindableConfigReply.ResourceBindableConfigStruct.valueOf(resourceConfigs.values()));
        } else {
            List<String> ids = resourceConfigs.keySet().stream().filter(it -> it.startsWith(msg.getCategory() + ".")).collect(Collectors.toList());
            ids.forEach(id -> results.add(APIGetResourceBindableConfigReply.ResourceBindableConfigStruct.valueOf(resourceConfigs.get(id))));
        }

        reply.setBindableConfigs(results);
        bus.reply(msg, reply);
    }

    private void handle(APIGetResourceConfigsMsg msg) {
        APIGetResourceConfigsReply reply = new APIGetResourceConfigsReply();
        List<String> identities = new ArrayList<>(msg.getNames().stream().map(msg::getIdentity).collect(Collectors.toList()));

        reply.setConfigs(new ArrayList<>());
        for (String identity : identities) {
            ResourceConfig rc = getResourceConfig(identity);
            List<ResourceConfigInventory> configs = rc.getEffectiveResourceConfigs(msg.getResourceUuid());
            ResourceConfigStruct struct = new ResourceConfigStruct();
            struct.setEffectiveConfigs(configs);
            struct.setName(rc.globalConfig.getName());
            struct.setValue(configs.isEmpty() ? rc.defaultValue(String.class) : configs.get(0).getValue());

            reply.getConfigs().add(struct);
        }

        bus.reply(msg, reply);

    }

    private void handle(APIGetResourceConfigMsg msg) {
        ResourceConfig rc = getResourceConfig(msg.getIdentity());
        APIGetResourceConfigReply reply = new APIGetResourceConfigReply();
        List<ResourceConfigInventory> configs = rc.getEffectiveResourceConfigs(msg.getResourceUuid());
        reply.setEffectiveConfigs(configs);
        reply.setValue(configs.isEmpty() ? rc.defaultValue(String.class) : configs.get(0).getValue());
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        BeanUtils.reflections.getFieldsAnnotatedWith(BindResourceConfig.class).forEach(field -> {
            try {
                buildResourceConfig(field);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });

        initResourceConfig();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public ResourceConfig getResourceConfig(String identity) {
        return resourceConfigs.get(identity);
    }

    @Override
    public <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz) {
        ResourceConfig rc = resourceConfigs.get(gc.getIdentity());
        if (rc == null) {
            logger.trace(String.format("no resource config[category:%s, name:%s] found, use global config instead", gc.getCategory(), gc.getName()));
            return gc.value(clz);
        }

        return rc.getResourceConfigValue(resourceUuid, clz);
    }

    @Override
    public <T> T getResourceConfigValueByResourceType(GlobalConfig gc, String resourceUuid, String resourceType, Class<T> clz) {
        ResourceConfig rc = resourceConfigs.get(gc.getIdentity());
        if (rc == null) {
            logger.trace(String.format("no resource config[category:%s, name:%s] found", gc.getCategory(), gc.getName()));
            return null;
        }
        return rc.getResourceConfigValueByResourceType(resourceUuid, resourceType, clz);
    }

    @Override
    public <T> Map<String, T> getResourceConfigValues(GlobalConfig gc, List<String> resourceUuids, Class<T> clz) {
        ResourceConfig rc = resourceConfigs.get(gc.getIdentity());
        if (rc == null) {
            logger.trace(String.format("no resource config[category:%s, name:%s] found, use global config instead", gc.getCategory(), gc.getName()));

            Map<String, T> result = new HashMap<>();
            resourceUuids.forEach(resourceUuid -> result.put(resourceUuid, gc.value(clz)));
            return result;
        }

        return rc.getResourceConfigValues(resourceUuids, clz);
    }

    public <T> Map<String, T> getResourceConfigValueByResourceUuids(GlobalConfig gc, List<String> resourceUuids, Class<T> clz) {
        Map<String, T> resourceConfigMap = new HashMap<>();

        resourceUuids.forEach(uuid -> {
            ResourceConfig rc = resourceConfigs.get(gc.getIdentity());
            if (rc == null) {
                logger.trace(String.format("no resource config[category:%s, name:%s] found, use global config instead", gc.getCategory(), gc.getName()));
                resourceConfigMap.put(uuid, gc.value(clz));
                return;
            }
            resourceConfigMap.put(uuid, rc.getResourceConfigValue(uuid, clz));
        });

        return resourceConfigMap;
    }

    protected void buildResourceConfig(Field field) throws Exception {
        BindResourceConfig at = field.getAnnotation(BindResourceConfig.class);
        GlobalConfig gc = (GlobalConfig) field.get(null);
        resourceConfigs.putIfAbsent(gc.getIdentity(), ResourceConfig.valueOf(gc, at));
    }

    private void initResourceConfig() {
        resourceConfigs.values().forEach(ResourceConfig::init);
    }
}
