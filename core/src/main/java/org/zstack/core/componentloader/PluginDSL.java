package org.zstack.core.componentloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 10/26/2015.
 */
public class PluginDSL {
    public static Map<Class, PluginDefinition> pluginDefinition = new HashMap<Class, PluginDefinition>();

    static Map<Class, PluginDefinition> getPluginDefinition() {
        return pluginDefinition;
    }

    static void setPluginDefinition(Map<Class, PluginDefinition> pluginDefinition) {
        PluginDSL.pluginDefinition = pluginDefinition;
    }

    public static class ExtensionDefinition {
        Class interfaceClass;
        int order = 0;
        Map<String, String> attributes = new HashMap<String, String>();

        public ExtensionDefinition extensionClass(Class extensionClass) {
            this.interfaceClass = extensionClass;
            return this;
        }

        public ExtensionDefinition order(int order) {
            this.order = order;
            return this;
        }

        public ExtensionDefinition attribute(String name, String value) {
            attributes.put(name, value);
            return this;
        }
    }

    public static class PluginDefinition {
        Class beanClass;
        List<ExtensionDefinition> extensions = new ArrayList<ExtensionDefinition>();

        public PluginDefinition(Class beanClass) {
            this.beanClass = beanClass;
            pluginDefinition.put(beanClass, this);
        }

        public ExtensionDefinition newExtension() {
            ExtensionDefinition ext = new ExtensionDefinition();
            extensions.add(ext);
            return ext;
        }
    }
}
