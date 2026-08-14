package org.zstack.core.plugin;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * PluginGlobalConfig implementation.
 */
@GlobalConfigDefinition
public class PluginGlobalConfig {
    public static final String CATEGORY = "plugin";

    @GlobalConfigValidation
    @GlobalConfigDef(type = Boolean.class, defaultValue = "true")
    public static GlobalConfig ALLOW_UNKNOWN_PRODUCT_PLUGIN =
            new GlobalConfig(CATEGORY, "allow.unknown.product.plugin");
}
