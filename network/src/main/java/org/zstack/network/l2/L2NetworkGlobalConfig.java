package org.zstack.network.l2;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class L2NetworkGlobalConfig {
    public static final String CATEGORY = "l2Network";

    @GlobalConfigValidation
    public static GlobalConfig DeleteL2BridgePhysically = new GlobalConfig(CATEGORY, "deleteL2.bridge");
}
