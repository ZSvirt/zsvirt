package org.zstack.header.core;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class MevocoCoreGlobalConfig {
    public static final String CATEGORY = "mevoco";

    @GlobalConfigDef(defaultValue = "false", description = "reject all scheduler job, ha and api which is not read only")
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig PAUSE_THE_WORLD = new GlobalConfig(CATEGORY, "pause.world");
}
