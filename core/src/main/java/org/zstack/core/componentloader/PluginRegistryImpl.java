package org.zstack.core.componentloader;

import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginDSL.ExtensionDefinition;
import org.zstack.core.componentloader.PluginDSL.PluginDefinition;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.*;


public class PluginRegistryImpl implements PluginRegistryIN, BannedModule {
    private static final CLogger logger = CLoggerImpl.getLogger(PluginRegistryImpl.class);
    private Map<String, List<PluginExtension>> extensions = new HashMap<>();
    private Map<String, List<PluginExtension>> extensionsByInterfaceName = new HashMap<>();
    private Map<Class, List> extensionsByInterfaceClass = new HashMap<>();
    private Map<Class, Map<Object, Object>> extensionAsMap = new HashMap<>();
    private Map<Class, Map<Object, List>> extensionListAsMap = new HashMap<>();

    private void sortPlugins() {
        for (List<PluginExtension> exts : extensionsByInterfaceName.values()) {
            // greater order means the position is more proceeding in plugin list
            exts.sort(Comparator.comparingInt(PluginExtension::getOrder).reversed());
        }
    }

    private void buildPluginTree() {
        ComponentLoader loader = Platform.getComponentLoader();
        for (Map.Entry<String, List<PluginExtension>> entry : extensions.entrySet()) {
            for (PluginExtension ext : entry.getValue()) {
                /*
                 * If instance-id is specified in extension declaration, find
                 * that bean used as extension implementation. Otherwise, use
                 * parent plugin as implementation
                 */
                try {
                    Class<?> interfaceClass = Class.forName(ext.getReferenceInterface());
                    Object instance;
                    if (!"".equals(ext.getInstanceId())) {
                        instance = loader.getComponentByBeanName(ext.getInstanceId());
                    } else {
                        instance = loader.getComponentByBeanName(ext.getBeanName());
                    }
                    ext.setInstance(instance);

                    String extModuleName = ext.getInstance().getClass().getCanonicalName();
                    if (isBannedModule(extModuleName)) {
                        continue;
                    }

                    if (!interfaceClass.isInstance(ext.getInstance())) {
                        throw new IllegalArgumentException(String.format("%s is not an instance of the interface %s",
                                extModuleName, interfaceClass.getName()));
                    }

                    List<PluginExtension> exts = extensionsByInterfaceName.get(ext.getReferenceInterface());
                    if (exts == null) {
                        exts = new ArrayList<>(1);
                    }
                    exts.add(ext);
                    extensionsByInterfaceName.put(ext.getReferenceInterface(), exts);
                } catch (Exception e) {
                    throw new CloudRuntimeException(
                            String.format("%s, mark extension referred to interface [%s] in bean[name=%s, class=%s] as invalid." +
                                            " Checking the bean XML file to fix it",
                                    e.getMessage(),
                                    ext.getReferenceInterface(),
                                    ext.getBeanName(),
                                    ext.getBeanClassName()),
                            e);
                }
            }
        }
    }

    @Override
    public void initialize() {
        buildPluginTree();
        continueBuildTreeFromDSL();
        sortPlugins();
        createClassPluginInstanceMap();
        logger.info("Plugin system has been initialized successfully");
    }

    private void continueBuildTreeFromDSL() {
        for (Map.Entry<Class, PluginDefinition> e : PluginDSL.getPluginDefinition().entrySet()) {
            Class beanClass = e.getKey();
            PluginDefinition definition = e.getValue();
            ComponentLoader loader = Platform.getComponentLoader();
            Object instance = loader.getComponent(beanClass);

            for (ExtensionDefinition extd : definition.extensions) {
                String ifaceName = extd.interfaceClass.getName();
                PluginExtension ext = new PluginExtension();
                ext.setInstance(instance);
                ext.setBeanClassName(instance.getClass().getName());
                ext.setOrder(extd.order);
                ext.setReferenceInterface(extd.interfaceClass.getName());
                ext.setAttributes(extd.attributes);

                if (!extd.interfaceClass.isInstance(ext.getInstance())) {
                    throw new IllegalArgumentException(
                            String.format("%s is not an instance of the interface %s",
                                    ext.getInstance().getClass().getCanonicalName(), extd.interfaceClass.getName()));
                }

                List<PluginExtension> exts = extensionsByInterfaceName.computeIfAbsent(ifaceName, any -> new ArrayList<>());

                logger.debug(String.format("Plugin[%s] declares an extension[%s] from static DSL",
                        beanClass.getName(), extd.interfaceClass.getName()));
                exts.add(ext);
            }
        }
    }

    private void createClassPluginInstanceMap() {
        for (Map.Entry<String, List<PluginExtension>> e : extensionsByInterfaceName.entrySet()) {
            String className = e.getKey();
            List<PluginExtension> exts = e.getValue();

            try {
                Class<?> clazz = Class.forName(className);
                List<Object> instances = new ArrayList<>();
                for (PluginExtension ext : exts) {
                    if (!instances.contains(ext.getInstance())) {
                        instances.add(ext.getInstance());
                    }
                }
                extensionsByInterfaceClass.put(clazz, instances);
            } catch (Exception ex) {
                throw new CloudRuntimeException(ex);
            }
        }
    }


    @Override
    public <T> List<T> getExtensionList(Class<T> clazz) {
        List<T> exts = extensionsByInterfaceClass.get(clazz);
        return exts == null ? new ArrayList<>() : exts;
    }

    @Override
    public <T, K> void saveExtensionAsMap(Class<T> clazz, Function<K, T> func) {
        List<T> exts = getExtensionList(clazz);
        Map<Object, Object> m = new HashMap<>();

        for (T ext : exts) {
            K key = func.call(ext);
            DebugUtils.Assert(key != null, "key cannot be null");

            m.put(key, ext);
        }

        extensionAsMap.put(clazz, m);
    }

    @Override
    public <T> T getExtensionFromMap(Object key, Class<T> clazz) {
        Map<Object, Object> m = extensionAsMap.get(clazz);
        if (m == null) {
            return null;
        }

        return (T) m.get(key);
    }

    @Override
    public <T, K> void saveExtensionListAsMap(Class<T> clazz, Function<K, T> func) {
        List<T> exts = getExtensionList(clazz);
        Map<Object, List> m = new HashMap<>();
        for (T ext : exts) {
            K key = func.call(ext);
            DebugUtils.Assert(key != null, "key cannot be null");

            List lst = m.computeIfAbsent(key, k -> new ArrayList<>());

            lst.add(ext);
        }

        extensionListAsMap.put(clazz, m);
    }

    @Override
    public <T> List getExtensionListFromMap(Object key, Class<T> clazz) {
        Map<Object, List> m = extensionListAsMap.get(clazz);
        if (m == null) {
            return new ArrayList<>();
        }

        return m.get(key);
    }

    @Override
    public void defineDynamicExtension(Class interfaceClass, Object instance) {
        List exts = extensionsByInterfaceClass.computeIfAbsent(interfaceClass, k -> new ArrayList<>());
        exts.add(instance);
    }

    @Override
    public List<PluginExtension> getExtensionByInterfaceName(String interfaceName) {
        List<PluginExtension> exts = extensionsByInterfaceName.get(interfaceName);
        if (exts == null) {
            exts = new ArrayList<>(0);
        }

        return exts;
    }

    public void setExtensions(Map<String, List<PluginExtension>> extensions) {
        this.extensions = extensions;
    }
}
