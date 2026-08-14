package org.zstack.imagereplicator;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

@GlobalConfigDefinition
public class ImageReplicatorGlobalConfig {
    public static final String CATEGORY = "imageReplication";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "Enable or disable image replication")
    public static PremiumGlobalConfig ENABLE_REPLICATION = new PremiumGlobalConfig(CATEGORY, "enable");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "60", type = Long.class, description = "Image replication scan interval in seconds")
    public static GlobalConfig ScanInterval = new GlobalConfig(CATEGORY, "scanInterval");
}
