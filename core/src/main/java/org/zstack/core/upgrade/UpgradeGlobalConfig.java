package org.zstack.core.upgrade;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class UpgradeGlobalConfig {
    public static final String CATEGORY = "upgradeControl";

    @GlobalConfigValidation
    public static GlobalConfig GRAYSCALE_UPGRADE = new GlobalConfig(CATEGORY, "grayscaleUpgrade");

    @GlobalConfigValidation
    public static GlobalConfig ALLOWED_API_LIST_GRAYSCALE_UPGRADING = new GlobalConfig(CATEGORY, "allowedApiListGrayscaleUpgrading");
}
