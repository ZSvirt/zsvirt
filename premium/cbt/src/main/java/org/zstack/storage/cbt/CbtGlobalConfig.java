package org.zstack.storage.cbt;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class CbtGlobalConfig {
    public static final String CATEGORY = "cbt";

    @GlobalConfigValidation(inNumberRange = {1, 16})
    @GlobalConfigDef(type = Integer.class, defaultValue = "5", description = "start CBT concurrency level")
    public static GlobalConfig START_CBT_CONCURRENT_LEVEL = new GlobalConfig(CATEGORY, "concurrent.start.level");
}
