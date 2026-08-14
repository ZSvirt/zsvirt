package org.zstack.core.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.abstraction.header.PluginDriver;
import org.zstack.abstraction.header.PluginValidator;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

public class PluginManager implements Component {
    private static final CLogger logger = Utils.getLogger(PluginManager.class);

    @Autowired
    private DatabaseFacade dbf;

    private final Set<Class<? extends PluginDriver>> pluginMetadata = new HashSet<>();
    private final Map<String, PluginDriver> pluginInstances = new HashMap<>();
    private final Map<Class<? extends PluginDriver>, List<PluginDriver>>
            pluginRegisters = new HashMap<>();
    private final Map<Class<? extends PluginDriver>, PluginValidator>
            pluginValidators = new HashMap<>();

    private void collectPluginProtocolMetadata() {
        Platform.getReflections().getSubTypesOf(PluginDriver.class).forEach(clz -> {
            if (!clz.getCanonicalName().contains("org.zstack.abstraction")
                    || !clz.isInterface()) {
                return;
            }

            if (pluginMetadata.contains(clz)) {
                throw new CloudRuntimeException(
                        String.format("duplicate PluginProtocol[name: %s]", clz));
            }

            pluginMetadata.add(clz);
        });
    }

    private void registerPluginAsSingleton(
            Class<? extends PluginDriver> pluginRegisterClz,
            Class<? extends PluginDriver> pluginDriverClz) {
        try {
            PluginDriver pluginDriver = pluginRegisterClz
                    .getConstructor()
                    .newInstance();
            if (pluginInstances.containsKey(pluginDriver.uuid())) {
                throw new CloudRuntimeException(
                        String.format("duplicate plugin[class: %s]", pluginRegisterClz));
            }

            if (pluginValidators.containsKey(pluginRegisterClz)) {
                pluginValidators.get(pluginRegisterClz).validate(pluginDriver);
            }

            // String format all String methods of plugin from pluginDriver to logger.debug
            logger.debug(String.format("register plugin[class: %s, productKey: %s, version: %s," +
                            " capabilities: %s, description: %s, vendor: %s, url: %s," +
                            " license: %s]",
                    pluginRegisterClz,
                    pluginDriver.uuid(),
                    pluginDriver.version(),
                    JSONObjectUtil.toJsonString(pluginDriver.features()),
                    pluginDriver.description(),
                    pluginDriver.vendor(),
                    pluginDriver.url(),
                    pluginDriver.license()));

            verifyPluginProduct(pluginDriver);

            pluginInstances.put(pluginDriver.uuid(), pluginDriver);
            pluginRegisters.computeIfAbsent(pluginDriverClz, k -> new ArrayList<>());
            pluginRegisters.get(pluginDriverClz).add(pluginDriver);

            PluginDriverVO vo = dbf.findByUuid(pluginDriver.uuid(), PluginDriverVO.class);
            if (vo == null) {
                vo = new PluginDriverVO();
                vo.setUuid(pluginDriver.uuid());
                vo.setName(pluginDriver.name());
                vo.setVendor(pluginDriver.vendor());
                vo.setFeatures(JSONObjectUtil.toJsonString(pluginDriver.features()));
                vo.setType(pluginDriver.type());
                dbf.persist(vo);
            } else {
                vo.setName(pluginDriver.name());
                vo.setVendor(pluginDriver.vendor());
                vo.setFeatures(JSONObjectUtil.toJsonString(pluginDriver.features()));
                vo.setType(pluginDriver.type());
                dbf.update(vo);
            }

        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void getPluginInterfaceSingletons(Class<? extends PluginDriver> abstractPluginClz) {
        Platform.getReflections()
                .getSubTypesOf(abstractPluginClz)
                .forEach(pluginDriverClz -> {
                    if (pluginDriverClz.isInterface()) {
                        return;
                    }

                    registerPluginAsSingleton(pluginDriverClz, abstractPluginClz);
                });
    }

    private void loadPluginsFromMetadata() {
        pluginMetadata.forEach(this::getPluginInterfaceSingletons);

        pluginRegisters.forEach((pluginClazz, instanceList) -> {
            PluginValidator validator = pluginValidators.get(pluginClazz);
            if (validator == null) {
                return;
            }

            validator.validateAllPlugins(instanceList);
        });
    }

    private void verifyPluginProduct(PluginDriver pluginDriver) {
        if (!PluginGlobalConfig.ALLOW_UNKNOWN_PRODUCT_PLUGIN.value(Boolean.class)
                && pluginDriver.uuid() == null) {
            throw new OperationFailureException(operr("unknown product plugin name: %s",
                    pluginDriver.name()));
        }

        if (pluginDriver.name() == null
                || pluginDriver.uuid() == null
                || pluginDriver.vendor() == null) {
            throw new OperationFailureException(operr("plugin[%s] name," +
                    " productKey and vendor cannot be null",
                    pluginDriver.getClass()));
        }

        doVerification(pluginDriver.name(), pluginDriver.uuid());
    }

    private void doVerification(String productName, String productKey) {
        // TODO: verify plugin driver
    }

    private void collectPluginValidators() {
        Platform.getReflections().getSubTypesOf(PluginValidator.class).forEach(clz -> {
            if (!clz.getCanonicalName().contains("org.zstack.abstraction")
                    || !clz.isInterface()) {
                return;
            }

            if (pluginMetadata.contains(clz)) {
                throw new CloudRuntimeException(
                        String.format("duplicate PluginValidator[name: %s]", clz));
            }

            try {
                PluginValidator pluginValidator = clz
                        .getConstructor()
                        .newInstance();

                pluginValidators.put(pluginValidator.pluginClass(), pluginValidator);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    @Override
    public boolean start() {
        collectPluginProtocolMetadata();
        collectPluginValidators();
        loadPluginsFromMetadata();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public boolean isFeatureSupported(
            String pluginProductKey,
            String capability) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("check plugin[%s] capability[%s]",
                    pluginProductKey, capability));
            logger.trace(String.format("plugin features %s",
                    JSONObjectUtil.toJsonString(pluginInstances
                            .get(pluginProductKey)
                            .features())));
            logger.trace(String.format("plugin feature state: %s",
                    JSONObjectUtil.toJsonString(pluginInstances
                            .get(pluginProductKey)
                            .features().get(capability) == Boolean.TRUE)));
        }

        return pluginInstances.get(pluginProductKey)
                .features()
                .get(capability) == Boolean.TRUE;
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginDriver> T getPlugin(String pluginProductKey) {
        if (!pluginInstances.containsKey(pluginProductKey)) {
            throw new CloudRuntimeException(String.format("Unsupported plugin %s",
                    pluginProductKey));
        }

        return (T) pluginInstances.get(pluginProductKey);
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginDriver> List<T> getPluginList(Class<? extends PluginDriver> pluginClass) {
        List<PluginDriver> plugins = pluginRegisters.get(pluginClass);
        return plugins != null ? (List<T>) plugins : new ArrayList<>();
    }

    public boolean isPluginTypeExist(Class<? extends PluginDriver> pluginClass, String type) {
        List<PluginDriver> plugins = pluginRegisters.get(pluginClass);
        if (plugins == null) {
            return false;
        }

        return plugins.stream()
                .anyMatch(plugin -> plugin.type().equals(type));
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginDriver> T getPlugin(Class<? extends PluginDriver> pluginClass, String type) {
        if (pluginRegisters.get(pluginClass)
                .stream()
                .filter(plugin -> plugin.type().equals(type))
                .count() > 1) {
            throw new CloudRuntimeException(String.format("multi plugin with same type %s", type));
        }

        return (T) pluginRegisters.get(pluginClass)
                .stream()
                .filter(plugin -> plugin.type().equals(type))
                .findFirst()
                .orElse(null);
    }
}
