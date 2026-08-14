package org.zstack.abstraction.header;

import java.util.Map;

/**
 * {@link PluginDriver} defines the specific requirements for plugin.
 * <p>
 *     We should get all metadata of the plugin.
 * </p>
 */
public interface PluginDriver {
    String type();

    /**
     * product name of your plugin
     * @return a String of name
     */
    String name();

    /**
     * unique product key from distribution
     * @return the key of current environment
     */
    String uuid();

    /**
     * plugin version
     * @return version of current plugin
     */
    String version();

    /**
     * capabilities map describe current plugin's whole
     * capabilities
     * @return map of feature
     */
    Map<String, Boolean> features();

    /**
     * plugin's description
     * @return description of current plugin
     */
    String description();

    /**
     * plugin's vendor
     * @return vendor of current plugin
     */
    String vendor();

    /**
     * plugin's url
     * @return url of current plugin
     */
    String url();

    /**
     * plugin's license
     * @return license of current plugin
     */
    String license();
}
